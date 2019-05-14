/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.util.OperatorNameConventions

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrElementToJsExpressionTransformer : BaseIrElementToJsNodeTransformer<JsExpression, JsGenerationContext> {

    override fun visitVararg(expression: IrVararg, context: JsGenerationContext): JsExpression {
        assert(expression.elements.none { it is IrSpreadElement })
        return JsArrayLiteral(expression.elements.map { it.accept(this, context) })
    }

    override fun visitExpressionBody(body: IrExpressionBody, context: JsGenerationContext): JsExpression =
        body.expression.accept(this, context)

    override fun visitFunctionReference(expression: IrFunctionReference, context: JsGenerationContext): JsExpression {
        val irFunction = expression.symbol.owner
        return irFunction.accept(IrFunctionToJsTransformer(), context).apply { name = null }
    }

    override fun <T> visitConst(expression: IrConst<T>, context: JsGenerationContext): JsExpression {
        val kind = expression.kind
        return when (kind) {
            is IrConstKind.String -> JsStringLiteral(kind.valueOf(expression))
            is IrConstKind.Null -> JsNullLiteral()
            is IrConstKind.Boolean -> JsBooleanLiteral(kind.valueOf(expression))
            is IrConstKind.Byte -> JsIntLiteral(kind.valueOf(expression).toInt())
            is IrConstKind.Short -> JsIntLiteral(kind.valueOf(expression).toInt())
            is IrConstKind.Int -> JsIntLiteral(kind.valueOf(expression))
            is IrConstKind.Long -> throw IllegalStateException("Long const should have been lowered at this point")
            is IrConstKind.Char -> throw IllegalStateException("Char const should have been lowered at this point")
            is IrConstKind.Float -> JsDoubleLiteral(toDoubleConst(kind.valueOf(expression)))
            is IrConstKind.Double -> JsDoubleLiteral(kind.valueOf(expression))
        }
    }

    private fun toDoubleConst(f: Float) = if (f.isInfinite() || f.isNaN()) f.toDouble() else f.toString().toDouble()

    override fun visitStringConcatenation(expression: IrStringConcatenation, context: JsGenerationContext): JsExpression {
        // TODO revisit
        return expression.arguments.fold<IrExpression, JsExpression>(JsStringLiteral("")) { jsExpr, irExpr ->
            JsBinaryOperation(
                JsBinaryOperator.ADD,
                jsExpr,
                irExpr.accept(this, context)
            )
        }
    }

    override fun visitGetField(expression: IrGetField, context: JsGenerationContext): JsExpression {
        val symbol = expression.symbol
        val field = symbol.owner

        val fieldParent = field.parent

        if (fieldParent is IrClass && field.isEffectivelyExternal()) {
            // External fields are only allowed in external enums
            assert(fieldParent.isEnumClass)
            return JsNameRef(
                field.getJsNameOrKotlinName().identifier,
                context.getNameForClass(fieldParent).makeRef()
            )
        }

        if (fieldParent is IrClass && fieldParent.isInline) {
            return expression.receiver!!.accept(this, context)
        }
        val fieldName = context.getNameForField(field)
        return JsNameRef(fieldName, expression.receiver?.accept(this, context))
    }

    override fun visitGetValue(expression: IrGetValue, context: JsGenerationContext): JsExpression =
        context.getNameForValueDeclaration(expression.symbol.owner).makeRef()

    override fun visitGetObjectValue(expression: IrGetObjectValue, context: JsGenerationContext) = when (expression.symbol.owner.kind) {
        ClassKind.OBJECT -> {
            val obj = expression.symbol.owner
            if (obj.isEffectivelyExternal()) {
                context.getRefForExternalClass(obj)
            } else {
                val className = context.getNameForClass(expression.symbol.owner)
                // TODO: Don't use implicit naming
                val getInstanceName = className.ident + "_getInstance"
                JsInvocation(JsNameRef(getInstanceName))
            }
        }
        else -> TODO()
    }


    override fun visitSetField(expression: IrSetField, context: JsGenerationContext): JsExpression {
        val fieldName = context.getNameForField(expression.symbol.owner)
        val dest = JsNameRef(fieldName, expression.receiver?.accept(this, context))
        val source = expression.value.accept(this, context)
        return jsAssignment(dest, source)
    }

    override fun visitSetVariable(expression: IrSetVariable, context: JsGenerationContext): JsExpression {
        val ref = JsNameRef(context.getNameForValueDeclaration(expression.symbol.owner))
        val value = expression.value.accept(this, context)
        return JsBinaryOperation(JsBinaryOperator.ASG, ref, value)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: JsGenerationContext): JsExpression {
        val classNameRef = context.getNameForConstructor(expression.symbol.owner).makeRef()
        val callFuncRef = JsNameRef(Namer.CALL_FUNCTION, classNameRef)
        val fromPrimary = context.currentFunction is IrConstructor
        val thisRef =
            if (fromPrimary) JsThisRef() else context.getNameForValueDeclaration(context.currentFunction!!.valueParameters.last()).makeRef()
        val arguments = translateCallArguments(expression, context)

        val constructor = expression.symbol.owner
        if (constructor.parentAsClass.isInline) {
            assert(constructor.isPrimary) {
                "Delegation to secondary inline constructors must be lowered into simple function calls"
            }
            return JsBinaryOperation(JsBinaryOperator.ASG, thisRef, arguments.single())
        }

        return JsInvocation(callFuncRef, listOf(thisRef) + arguments)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, context: JsGenerationContext): JsExpression {
        val function = expression.symbol.owner
        val symbol = expression.symbol

        context.staticContext.intrinsics[symbol]?.let {
            return it(expression, context)
        }

        val arguments = translateCallArguments(expression, context)
        val klass = function.parentAsClass
        return if (klass.isInline) {
            assert(function.isPrimary) {
                "Inline class secondary constructors must be lowered into static methods"
            }
            // Argument value constructs unboxed inline class instance
            arguments.single()
        } else {
            val ref = when {
                klass.isEffectivelyExternal() ->
                    context.getRefForExternalClass(klass)

                else ->
                    context.getNameForClass(klass).makeRef()
            }
            JsNew(ref, arguments)
        }
    }

    override fun visitCall(expression: IrCall, context: JsGenerationContext): JsExpression {
        val function = expression.symbol.owner.realOverrideTarget
        val symbol = function.symbol

        context.staticContext.intrinsics[symbol]?.let {
            return it(expression, context)
        }

        val jsDispatchReceiver = expression.dispatchReceiver?.accept(this, context)
        val jsExtensionReceiver = expression.extensionReceiver?.accept(this, context)
        val arguments = translateCallArguments(expression, context)

        // Transform external property accessor call
        // @JsName-annotated external property accessors are translated as function calls
        if (function is IrSimpleFunction && function.getJsName() == null) {
            val property = function.correspondingPropertySymbol?.owner
            if (property != null && property.isEffectivelyExternal()) {
                val nameRef = JsNameRef(context.getNameForProperty(property), jsDispatchReceiver)
                return when (function) {
                    property.getter -> nameRef
                    property.setter -> jsAssignment(nameRef, arguments.single())
                    else -> error("Function must be an accessor of corresponding property")
                }
            }
        }

        if (isNativeInvoke(expression)) {
            return JsInvocation(jsDispatchReceiver!!, arguments)
        }

        expression.superQualifierSymbol?.let { superQualifier ->
            require(function is IrSimpleFunction)

            val (target, klass) = if (superQualifier.owner.isInterface) {
                val impl = function.resolveFakeOverride()!!
                Pair(impl, impl.parentAsClass)
            } else {
                Pair(function, superQualifier.owner)
            }

            val qualifierName = context.getNameForClass(klass).makeRef()
            val targetName = context.getNameForMemberFunction(target)
            val qPrototype = JsNameRef(targetName, prototypeOf(qualifierName))
            val callRef = JsNameRef(Namer.CALL_FUNCTION, qPrototype)
            return JsInvocation(callRef, jsDispatchReceiver?.let { receiver -> listOf(receiver) + arguments } ?: arguments)
        }

        val varargParameterIndex = function.valueParameters.indexOfFirst { it.varargElementType != null }
        val isExternalVararg = function.isEffectivelyExternal() && varargParameterIndex != -1


        if (function is IrConstructor) {
            // Inline class primary constructor takes a single value of to
            // initialize underlying property.
            // TODO: Support initialization block
            val klass = function.parentAsClass
            return if (klass.isInline) {
                assert(function.isPrimary) {
                    "Inline class secondary constructors must be lowered into static methods"
                }
                // Argument value constructs unboxed inline class instance
                arguments.single()
            } else {
                val ref = when {
                    klass.isEffectivelyExternal() ->
                        context.getRefForExternalClass(klass)

                    else ->
                        context.getNameForClass(klass).makeRef()
                }
                JsNew(ref, arguments)
            }
        }

        require(function is IrSimpleFunction)

        val symbolName = when (jsDispatchReceiver) {
            null -> context.getNameForStaticFunction(function)
            else -> context.getNameForMemberFunction(function)
        }

        val ref = when (jsDispatchReceiver) {
            null -> JsNameRef(symbolName)
            else -> JsNameRef(symbolName, jsDispatchReceiver)
        }

        return if (isExternalVararg) {

            // External vararg arguments should be represented in JS as multiple "plain" arguments (opposed to arrays in Kotlin)
            // We are using `Function.prototype.apply` function to pass all arguments as a single array.
            // For this purpose are concatenating non-vararg arguments with vararg.
            // TODO: Don't use `Function.prototype.apply` when number of arguments is known at compile time (e.g. there are no spread operators)
            val arrayConcat = JsNameRef("concat", JsArrayLiteral())
            val arraySliceCall = JsNameRef("call", JsNameRef("slice", JsArrayLiteral()))

            val argumentsAsSingleArray = JsInvocation(
                arrayConcat,
                listOfNotNull(jsExtensionReceiver) + arguments.mapIndexed { index, argument ->
                    when (index) {

                        // Call `Array.prototype.slice` on vararg arguments in order to convert array-like objects into proper arrays
                        // TODO: Optimize for proper arrays
                        varargParameterIndex -> JsInvocation(arraySliceCall, argument)

                        // TODO: Don't wrap non-array-like arguments with array literal
                        // TODO: Wrap adjacent non-vararg arguments in a single array literal
                        else -> JsArrayLiteral(listOf(argument))
                    }
                }
            )

            if (jsDispatchReceiver != null) {
                // TODO: Do not create IIFE when receiver expression is simple or has no side effects
                // TODO: Do not create IIFE at all? (Currently there is no reliable way to create temporary variable in current scope)
                val receiverName = context.currentScope.declareFreshName("\$externalVarargReceiverTmp")
                val receiverRef = receiverName.makeRef()
                JsInvocation(
                    // Create scope for temporary variable holding dispatch receiver
                    // It is used both during method reference and passing `this` value to `apply` function.
                    JsFunction(
                        context.currentScope,
                        JsBlock(
                            JsVars(JsVars.JsVar(receiverName, jsDispatchReceiver)),
                            JsReturn(
                                JsInvocation(
                                    JsNameRef("apply", JsNameRef(symbolName, receiverRef)),
                                    listOf(
                                        receiverRef,
                                        argumentsAsSingleArray
                                    )
                                )
                            )
                        ),
                        "VarargIIFE"
                    )
                )
            } else {
                JsInvocation(
                    JsNameRef("apply", JsNameRef(symbolName)),
                    listOf(JsNullLiteral(), argumentsAsSingleArray)
                )
            }
        } else {
            JsInvocation(ref, listOfNotNull(jsExtensionReceiver) + arguments)
        }
    }

    override fun visitWhen(expression: IrWhen, context: JsGenerationContext): JsExpression {
        val lastBranch = expression.branches.lastOrNull()
        val implicitElse =
            if (lastBranch == null || !isElseBranch(lastBranch))
                JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(0))
            else
                null

        assert(implicitElse == null || expression.type.isUnit()) { "Non unit when-expression must have else branch" }

        return expression.toJsNode(this, context, ::JsConditional, implicitElse)!!
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: JsGenerationContext): JsExpression {
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_CAST -> expression.argument.accept(this, data)
            else -> throw IllegalStateException("All type operator calls except IMPLICIT_CAST should be lowered at this point")
        }
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: JsGenerationContext): JsExpression =
        JsNameRef(expression.memberName, expression.receiver.accept(this, data))

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: JsGenerationContext): JsExpression =
        when (expression.operator) {
            IrDynamicOperator.UNARY_PLUS -> prefixOperation(JsUnaryOperator.POS, expression, data)
            IrDynamicOperator.UNARY_MINUS -> prefixOperation(JsUnaryOperator.NEG, expression, data)

            IrDynamicOperator.EXCL -> prefixOperation(JsUnaryOperator.NOT, expression, data)

            IrDynamicOperator.PREFIX_INCREMENT -> prefixOperation(JsUnaryOperator.INC, expression, data)
            IrDynamicOperator.PREFIX_DECREMENT -> prefixOperation(JsUnaryOperator.DEC, expression, data)

            IrDynamicOperator.POSTFIX_INCREMENT -> postfixOperation(JsUnaryOperator.INC, expression, data)
            IrDynamicOperator.POSTFIX_DECREMENT -> postfixOperation(JsUnaryOperator.DEC, expression, data)

            IrDynamicOperator.BINARY_PLUS -> binaryOperation(JsBinaryOperator.ADD, expression, data)
            IrDynamicOperator.BINARY_MINUS -> binaryOperation(JsBinaryOperator.SUB, expression, data)
            IrDynamicOperator.MUL -> binaryOperation(JsBinaryOperator.MUL, expression, data)
            IrDynamicOperator.DIV -> binaryOperation(JsBinaryOperator.DIV, expression, data)
            IrDynamicOperator.MOD -> binaryOperation(JsBinaryOperator.MOD, expression, data)

            IrDynamicOperator.GT -> binaryOperation(JsBinaryOperator.GT, expression, data)
            IrDynamicOperator.LT -> binaryOperation(JsBinaryOperator.LT, expression, data)
            IrDynamicOperator.GE -> binaryOperation(JsBinaryOperator.GTE, expression, data)
            IrDynamicOperator.LE -> binaryOperation(JsBinaryOperator.LTE, expression, data)

            IrDynamicOperator.EQEQ -> binaryOperation(JsBinaryOperator.EQ, expression, data)
            IrDynamicOperator.EXCLEQ -> binaryOperation(JsBinaryOperator.NEQ, expression, data)

            IrDynamicOperator.EQEQEQ -> binaryOperation(JsBinaryOperator.REF_EQ, expression, data)
            IrDynamicOperator.EXCLEQEQ -> binaryOperation(JsBinaryOperator.REF_NEQ, expression, data)

            IrDynamicOperator.ANDAND -> binaryOperation(JsBinaryOperator.AND, expression, data)
            IrDynamicOperator.OROR -> binaryOperation(JsBinaryOperator.OR, expression, data)

            IrDynamicOperator.EQ -> binaryOperation(JsBinaryOperator.ASG, expression, data)
            IrDynamicOperator.PLUSEQ -> binaryOperation(JsBinaryOperator.ASG_ADD, expression, data)
            IrDynamicOperator.MINUSEQ -> binaryOperation(JsBinaryOperator.ASG_SUB, expression, data)
            IrDynamicOperator.MULEQ -> binaryOperation(JsBinaryOperator.ASG_MUL, expression, data)
            IrDynamicOperator.DIVEQ -> binaryOperation(JsBinaryOperator.ASG_DIV, expression, data)
            IrDynamicOperator.MODEQ -> binaryOperation(JsBinaryOperator.ASG_MOD, expression, data)

            IrDynamicOperator.ARRAY_ACCESS -> JsArrayAccess(expression.left.accept(this, data), expression.right.accept(this, data))

            IrDynamicOperator.INVOKE ->
                JsInvocation(
                    expression.receiver.accept(this, data),
                    expression.arguments.map { it.accept(this, data) }
                )

            else -> error("Unexpected operator ${expression.operator}: ${expression.render()}")
        }

    private fun prefixOperation(operator: JsUnaryOperator, expression: IrDynamicOperatorExpression, data: JsGenerationContext) =
        JsPrefixOperation(
            operator,
            expression.receiver.accept(this, data)
        )

    private fun postfixOperation(operator: JsUnaryOperator, expression: IrDynamicOperatorExpression, data: JsGenerationContext) =
        JsPostfixOperation(
            operator,
            expression.receiver.accept(this, data)
        )

    private fun binaryOperation(operator: JsBinaryOperator, expression: IrDynamicOperatorExpression, data: JsGenerationContext) =
        JsBinaryOperation(
            operator,
            expression.left.accept(this, data),
            expression.right.accept(this, data)
        )

    private fun isNativeInvoke(call: IrCall): Boolean {
        val simpleFunction = call.symbol.owner as? IrSimpleFunction ?: return false
        val receiverType = simpleFunction.dispatchReceiverParameter?.type ?: return false

        if (simpleFunction.isSuspend) return false

        return simpleFunction.name == OperatorNameConventions.INVOKE && receiverType.isFunctionTypeOrSubtype()
    }
}

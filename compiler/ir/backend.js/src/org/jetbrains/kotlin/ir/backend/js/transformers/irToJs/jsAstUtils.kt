/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.ir.backend.js.lower.InteropCallableReferenceLowering
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.util.OperatorNameConventions

fun jsVar(name: JsName, initializer: IrExpression?, context: JsGenerationContext): JsVars {
    val jsInitializer = initializer?.accept(IrElementToJsExpressionTransformer(), context)
    return JsVars(JsVars.JsVar(name, jsInitializer))
}

fun <T : JsNode> IrWhen.toJsNode(
    tr: BaseIrElementToJsNodeTransformer<T, JsGenerationContext>,
    data: JsGenerationContext,
    node: (JsExpression, T, T?) -> T,
    implicitElse: T? = null
): T? =
    branches.foldRight(implicitElse) { br, n ->
        val body = br.result.accept(tr, data)
        if (isElseBranch(br)) body
        else {
            val condition = br.condition.accept(IrElementToJsExpressionTransformer(), data)
            node(condition, body, n)
        }
    }

fun jsAssignment(left: JsExpression, right: JsExpression) = JsBinaryOperation(JsBinaryOperator.ASG, left, right)

fun prototypeOf(classNameRef: JsExpression) = JsNameRef(Namer.PROTOTYPE_NAME, classNameRef)

fun translateFunction(declaration: IrFunction, name: JsName?, context: JsGenerationContext): JsFunction {
    val functionContext = context.newDeclaration(declaration)
    val functionParams = declaration.valueParameters.map { functionContext.getNameForValueDeclaration(it) }
    val body = declaration.body?.accept(IrElementToJsStatementTransformer(), functionContext) as? JsBlock ?: JsBlock()

    val function = JsFunction(emptyScope, body, "member function ${name ?: "annon"}")

    function.name = name

    fun JsFunction.addParameter(parameter: JsName) {
        parameters.add(JsParameter(parameter))
    }

    declaration.extensionReceiverParameter?.let { function.addParameter(functionContext.getNameForValueDeclaration(it)) }
    functionParams.forEach { function.addParameter(it) }
    if (declaration.isSuspend) {
        function.addParameter(JsName(Namer.CONTINUATION)) // TODO: Use namer?
    }

    return function
}

private fun isNativeInvoke(receiver: JsExpression?, call: IrCall): Boolean {
    if (receiver == null || receiver is JsThisRef) return false
    val simpleFunction = call.symbol.owner as? IrSimpleFunction ?: return false
    val receiverType = simpleFunction.dispatchReceiverParameter?.type ?: return false

    if (call.origin === InteropCallableReferenceLowering.Companion.EXPLICIT_INVOKE) return false

    return simpleFunction.name == OperatorNameConventions.INVOKE && receiverType.isFunctionTypeOrSubtype()
}

fun translateCall(
    expression: IrCall,
    context: JsGenerationContext,
    transformer: IrElementToJsExpressionTransformer
): JsExpression {
    val function = expression.symbol.owner.realOverrideTarget

    context.staticContext.intrinsics[function.symbol]?.let {
        return it(expression, context)
    }

    val jsDispatchReceiver = expression.dispatchReceiver?.accept(transformer, context)
    val jsExtensionReceiver = expression.extensionReceiver?.accept(transformer, context)
    val arguments = translateCallArguments(expression, context, transformer)

    // Transform external property accessor call
    // @JsName-annotated external property accessors are translated as function calls
    if (function.getJsName() == null) {
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

    if (isNativeInvoke(jsDispatchReceiver, expression)) {
        return JsInvocation(jsDispatchReceiver!!, arguments)
    }

    expression.superQualifierSymbol?.let { superQualifier ->
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
            val receiverName = JsName("\$externalVarargReceiverTmp")
            val receiverRef = receiverName.makeRef()
            JsInvocation(
                // Create scope for temporary variable holding dispatch receiver
                // It is used both during method reference and passing `this` value to `apply` function.
                JsNameRef(
                    "call",
                    JsFunction(
                        emptyScope,
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
                ),
                JsThisRef()
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

fun translateCallArguments(expression: IrMemberAccessExpression<*>, context: JsGenerationContext, transformer: IrElementToJsExpressionTransformer): List<JsExpression> {
    val size = expression.valueArgumentsCount

    val arguments = (0 until size).mapTo(ArrayList(size)) { index ->
        val argument = expression.getValueArgument(index)
        val result = argument?.accept(transformer, context)
        if (result == null) {
            if (context.staticContext.backendContext.es6mode) return@mapTo JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(2))

            assert(expression is IrFunctionAccessExpression && expression.symbol.owner.isExternalOrInheritedFromExternal())
            JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(1))
        } else
            result
    }

    return if (expression.symbol.isSuspend) {
        arguments + context.continuation
    } else arguments
}

fun JsStatement.asBlock() = this as? JsBlock ?: JsBlock(this)

fun defineProperty(receiver: JsExpression, name: String, value: () -> JsExpression): JsInvocation {
    val objectDefineProperty = JsNameRef("defineProperty", Namer.JS_OBJECT)
    return JsInvocation(objectDefineProperty, receiver, JsStringLiteral(name), value())
}

fun defineProperty(receiver: JsExpression, name: String, getter: JsExpression?, setter: JsExpression? = null) =
    defineProperty(receiver, name) {
        JsObjectLiteral(true).apply {
            propertyInitializers += JsPropertyInitializer(JsStringLiteral("configurable"), JsBooleanLiteral(true))
            if (getter != null)
                propertyInitializers += JsPropertyInitializer(JsStringLiteral("get"), getter)
            if (setter != null)
                propertyInitializers += JsPropertyInitializer(JsStringLiteral("set"), setter)
        }
    }


// Partially copied from org.jetbrains.kotlin.js.translate.utils.JsAstUtils
object JsAstUtils {
    private fun deBlockIfPossible(statement: JsStatement): JsStatement {
        return if (statement is JsBlock && statement.statements.size == 1) {
            statement.statements[0]
        } else {
            statement
        }
    }

    fun newJsIf(
        ifExpression: JsExpression,
        thenStatement: JsStatement,
        elseStatement: JsStatement? = null
    ): JsIf {
        return JsIf(ifExpression, deBlockIfPossible(thenStatement), elseStatement?.let { deBlockIfPossible(it) })
    }

    fun and(op1: JsExpression, op2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.AND, op1, op2)
    }

    fun or(op1: JsExpression, op2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.OR, op1, op2)
    }

    fun equality(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.REF_EQ, arg1, arg2)
    }

    fun inequality(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.REF_NEQ, arg1, arg2)
    }

    fun lessThanEq(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.LTE, arg1, arg2)
    }

    fun lessThan(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.LT, arg1, arg2)
    }

    fun greaterThan(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.GT, arg1, arg2)
    }

    fun greaterThanEq(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.GTE, arg1, arg2)
    }

    fun assignment(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.ASG, left, right)
    }

    fun assignmentToThisField(fieldName: String, right: JsExpression): JsStatement {
        return assignment(JsNameRef(fieldName, JsThisRef()), right).source(right.source).makeStmt()
    }

    fun decomposeAssignment(expr: JsExpression): Pair<JsExpression, JsExpression>? {
        if (expr !is JsBinaryOperation) return null

        return if (expr.operator != JsBinaryOperator.ASG) null else Pair(expr.arg1, expr.arg2)

    }

    fun sum(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.ADD, left, right)
    }

    fun addAssign(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.ASG_ADD, left, right)
    }

    fun subtract(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.SUB, left, right)
    }

    fun mul(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.MUL, left, right)
    }

    fun div(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.DIV, left, right)
    }

    fun mod(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.MOD, left, right)
    }

    fun not(expression: JsExpression): JsPrefixOperation {
        return JsPrefixOperation(JsUnaryOperator.NOT, expression)
    }

    fun typeOfIs(expression: JsExpression, string: JsStringLiteral): JsBinaryOperation {
        return equality(JsPrefixOperation(JsUnaryOperator.TYPEOF, expression), string)
    }

    fun newVar(name: JsName, expr: JsExpression?): JsVars {
        return JsVars(JsVars.JsVar(name, expr))
    }
}

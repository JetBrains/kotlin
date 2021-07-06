/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.js.backend.ast.*

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrElementToJsExpressionTransformer : BaseIrElementToJsNodeTransformer<JsExpression, JsGenerationContext> {

    override fun visitComposite(expression: IrComposite, data: JsGenerationContext): JsExpression {
        val size = expression.statements.size
        if (size == 0) TODO("Empty IrComposite is not supported")

        val first = expression.statements[0].accept(this, data)
        if (size == 1) return first

        return expression.statements.fold(first) { left, right ->
            JsBinaryOperation(JsBinaryOperator.COMMA, left, right.accept(this, data))
        }
    }

    override fun visitVararg(expression: IrVararg, context: JsGenerationContext): JsExpression {
        assert(expression.elements.none { it is IrSpreadElement })
        return JsArrayLiteral(expression.elements.map { it.accept(this, context) }).withSource(expression, context)
    }

    override fun visitExpressionBody(body: IrExpressionBody, context: JsGenerationContext): JsExpression =
        body.expression.accept(this, context)

    override fun visitFunctionExpression(expression: IrFunctionExpression, context: JsGenerationContext): JsExpression {
        val irFunction = expression.function
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
        }.withSource(expression, context)
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
            assert(fieldParent.isEnumClass) {
                "${field.render()} in non-external class ${fieldParent.render()}"
            }
            return JsNameRef(
                field.getJsNameOrKotlinName().identifier,
                context.getNameForClass(fieldParent).makeRef()
            ).withSource(expression, context)
        }

        if (fieldParent is IrClass && fieldParent.isInline) {
            return expression.receiver!!.accept(this, context).withSource(expression, context)
        }
        val fieldName = context.getNameForField(field)
        return JsNameRef(fieldName, expression.receiver?.accept(this, context)).withSource(expression, context)
    }

    override fun visitGetValue(expression: IrGetValue, context: JsGenerationContext): JsExpression {
        if (expression.symbol.owner.isThisReceiver()) return JsThisRef().withSource(expression, context)
        return context.getNameForValueDeclaration(expression.symbol.owner).makeRef().withSource(expression, context)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, context: JsGenerationContext): JsExpression {
        val obj = expression.symbol.owner
        assert(obj.kind == ClassKind.OBJECT)
        assert(obj.isEffectivelyExternal()) { "Non external IrGetObjectValue must be lowered" }

        return context.getRefForExternalClass(obj).withSource(expression, context)
    }

    override fun visitSetField(expression: IrSetField, context: JsGenerationContext): JsExpression {
        val fieldName = context.getNameForField(expression.symbol.owner)
        val dest = JsNameRef(fieldName, expression.receiver?.accept(this, context))
        val source = expression.value.accept(this, context)
        return jsAssignment(dest, source).withSource(expression, context)
    }

    override fun visitSetValue(expression: IrSetValue, context: JsGenerationContext): JsExpression {
        val ref = JsNameRef(context.getNameForValueDeclaration(expression.symbol.owner))
        val value = expression.value.accept(this, context)
        return JsBinaryOperation(JsBinaryOperator.ASG, ref, value).withSource(expression, context)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: JsGenerationContext): JsExpression {
        val classNameRef = context.getNameForConstructor(expression.symbol.owner).makeRef()
        val callFuncRef = JsNameRef(Namer.CALL_FUNCTION, classNameRef)
        val fromPrimary = context.currentFunction is IrConstructor
        val thisRef =
            if (fromPrimary) JsThisRef() else context.getNameForValueDeclaration(context.currentFunction!!.valueParameters.last()).makeRef()
        val arguments = translateCallArguments(expression, context, this)

        val constructor = expression.symbol.owner
        if (constructor.parentAsClass.isInline) {
            assert(constructor.isPrimary) {
                "Delegation to secondary inline constructors must be lowered into simple function calls"
            }
            return JsBinaryOperation(JsBinaryOperator.ASG, thisRef, arguments.single()).withSource(expression, context)
        }

        return if (context.staticContext.backendContext.es6mode) {
            JsInvocation(JsNameRef("super"), arguments)
        } else {
            JsInvocation(callFuncRef, listOf(thisRef) + arguments)
        }.withSource(expression, context)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, context: JsGenerationContext): JsExpression {
        val function = expression.symbol.owner
        val arguments = translateCallArguments(expression, context, this)
        val klass = function.parentAsClass

        require(!klass.isInline) {
            "All inline class constructor calls must be lowered to static function calls"
        }

        return when {
            klass.isEffectivelyExternal() -> {
                val refForExternalClass = context.getRefForExternalClass(klass)
                val varargParameterIndex = expression.symbol.owner.varargParameterIndex()
                if (varargParameterIndex == -1) {
                    JsNew(refForExternalClass, arguments)
                } else {
                    val argumentsAsSingleArray = argumentsWithVarargAsSingleArray(
                        expression,
                        context,
                        JsNullLiteral(),
                        arguments,
                        varargParameterIndex
                    )
                    JsNew(
                        JsInvocation(
                            JsNameRef("apply", JsNameRef("bind", JsNameRef("Function"))),
                            refForExternalClass,
                            argumentsAsSingleArray
                        ),
                        emptyList()
                    )
                }
            }
            else -> {
                val ref = context.getNameForClass(klass).makeRef()
                JsNew(ref, arguments)
            }
        }.withSource(expression, context)
    }

    override fun visitCall(expression: IrCall, context: JsGenerationContext): JsExpression {
        if (context.checkIfJsCode(expression.symbol)) {
            val statements = translateJsCodeIntoStatementList(expression.getValueArgument(0) ?: error("JsCode is expected"))!!
            if (statements.isEmpty()) return JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(3)) // TODO: report warning or even error

            val lastStatement = statements.last()
            if (statements.size == 1) {
                if (lastStatement is JsExpressionStatement) return lastStatement.expression.withSource(expression, context)
            }

            val newStatements = statements.toMutableList()

            when (lastStatement) {
                is JsReturn -> {
                }
                is JsExpressionStatement -> {
                    newStatements[statements.lastIndex] = JsReturn(lastStatement.expression)
                }
                // TODO: report warning or even error
                else -> newStatements += JsReturn(JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(3)))
            }

            val syntheticFunction = JsFunction(emptyScope, JsBlock(newStatements), "")
            return JsInvocation(syntheticFunction).withSource(expression, context)

        }
        return translateCall(expression, context, this).withSource(expression, context)
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
            IrTypeOperator.REINTERPRET_CAST -> expression.argument.accept(this, data)
            else -> error("All type operator calls except REINTERPRET_CAST should be lowered at this point: ${expression.operator}")
        }.withSource(expression, data)
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: JsGenerationContext): JsExpression =
        JsNameRef(expression.memberName, expression.receiver.accept(this, data)).withSource(expression, data)

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
        }.withSource(expression, data)

    override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: JsGenerationContext): JsExpression {
        val name = when (val function = expression.symbol.owner) {
            is IrConstructor -> data.getNameForConstructor(function)
            is IrSimpleFunction -> data.getNameForStaticFunction(function)
            else -> error("Unexpected function kind")
        }
        return JsNameRef(name).withSource(expression, data)
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

    private fun IrValueDeclaration.isThisReceiver(): Boolean = this !is IrVariable && when (val p = parent) {
        is IrSimpleFunction -> this === p.dispatchReceiverParameter
        is IrClass -> this === p.thisReceiver
        else -> false
    }
}

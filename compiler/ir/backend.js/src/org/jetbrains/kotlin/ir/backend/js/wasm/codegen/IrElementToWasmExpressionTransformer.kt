/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.js.backend.ast.*

class IrElementToWasmExpressionTransformer : BaseIrElementToWasmNodeTransformer<JsExpression, WasmStaticContext> {
    override fun visitVararg(expression: IrVararg, data: WasmStaticContext): JsExpression {
        assert(expression.elements.none { it is IrSpreadElement })
        return JsArrayLiteral(expression.elements.map { it.accept(this, data) })
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: WasmStaticContext): JsExpression =
        body.expression.accept(this, data)

    override fun visitFunctionReference(expression: IrFunctionReference, data: WasmStaticContext): JsExpression {
        val irFunction = expression.symbol.owner
        return (irFunction.accept(IrFunctionToWasmTransformer(), data) as JsFunction).apply { name = null }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: WasmStaticContext): JsExpression {
        val kind = expression.kind
        return when (kind) {
            is IrConstKind.String -> JsStringLiteral(kind.valueOf(expression))
            is IrConstKind.Null -> JsNullLiteral()
            is IrConstKind.Boolean -> JsBooleanLiteral(kind.valueOf(expression))
            is IrConstKind.Byte -> JsIntLiteral(kind.valueOf(expression).toInt())
            is IrConstKind.Short -> JsIntLiteral(kind.valueOf(expression).toInt())
            is IrConstKind.Int -> JsIntLiteral(kind.valueOf(expression))
            is IrConstKind.Long -> JsStringLiteral("WASM: TODO: Long const should have been lowered at this point")
            is IrConstKind.Char -> JsStringLiteral("WASM: TODO: Char const should have been lowered at this point")
            is IrConstKind.Float -> JsDoubleLiteral(toDoubleConst(kind.valueOf(expression)))
            is IrConstKind.Double -> JsDoubleLiteral(kind.valueOf(expression))
        }
    }

    private fun toDoubleConst(f: Float) = if (f.isInfinite() || f.isNaN()) f.toDouble() else f.toString().toDouble()

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: WasmStaticContext): JsExpression {
        // TODO revisit
        return expression.arguments.fold<IrExpression, JsExpression>(JsStringLiteral("")) { jsExpr, irExpr ->
            JsBinaryOperation(
                JsBinaryOperator.ADD,
                jsExpr,
                irExpr.accept(this, data)
            )
        }
    }

    override fun visitGetField(expression: IrGetField, data: WasmStaticContext): JsExpression {
        val symbol = expression.symbol
        val field = symbol.owner

        val fieldParent = field.parent

        if (fieldParent is IrClass && fieldParent.isInline) {
            return expression.receiver!!.accept(this, data)
        }
        val fieldName = data.getNameForField(field)
        return JsNameRef(fieldName, expression.receiver?.accept(this, data))
    }

    override fun visitGetValue(expression: IrGetValue, data: WasmStaticContext): JsExpression =
        data.getNameForValueDeclaration(expression.symbol.owner).makeRef()

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: WasmStaticContext): JsExpression {
        val obj = expression.symbol.owner
        assert(obj.kind == ClassKind.OBJECT)
        assert(obj.isEffectivelyExternal()) { "Non external IrGetObjectValue must be lowered" }
        return data.getRefForExternalClass(obj)
    }

    override fun visitSetField(expression: IrSetField, data: WasmStaticContext): JsExpression {
        val fieldName = data.getNameForField(expression.symbol.owner)
        val dest = JsNameRef(fieldName, expression.receiver?.accept(this, data))
        val source = expression.value.accept(this, data)
        return jsAssignment(dest, source)
    }

    override fun visitSetVariable(expression: IrSetVariable, data: WasmStaticContext): JsExpression {
        val ref = JsNameRef(data.getNameForValueDeclaration(expression.symbol.owner))
        val value = expression.value.accept(this, data)
        return JsBinaryOperation(JsBinaryOperator.ASG, ref, value)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: WasmStaticContext): JsExpression {
        return JsStringLiteral("Delegating constructor calls are not supported")
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: WasmStaticContext): JsExpression {
        val function = expression.symbol.owner
        val arguments = translateCallArguments(expression, data, this)
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
                    data.getRefForExternalClass(klass)

                else ->
                    data.getNameForClass(klass).makeRef()
            }
            JsNew(ref, arguments)
        }
    }

    override fun visitCall(expression: IrCall, context: WasmStaticContext): JsExpression {
        return translateCall(expression, context, this)
    }

    override fun visitWhen(expression: IrWhen, context: WasmStaticContext): JsExpression {
        val lastBranch = expression.branches.lastOrNull()
        val implicitElse =
            if (lastBranch == null || !isElseBranch(lastBranch))
                JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(0))
            else
                null

        assert(implicitElse == null || expression.type.isUnit()) { "Non unit when-expression must have else branch" }

        return expression.toJsNode(this, context, ::JsConditional, implicitElse)!!
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: WasmStaticContext): JsExpression {
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_CAST -> expression.argument.accept(this, data)
            else -> expression.argument.accept(this, data)
            // TODO: WASM
            // else -> error("All type operator calls except IMPLICIT_CAST should be lowered at this point")
        }
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: WasmStaticContext): JsExpression {
        return JsStringLiteral("WASM: TODO: IrGetEnumValue")
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: WasmStaticContext): JsExpression =
        JsNameRef(expression.memberName, expression.receiver.accept(this, data))

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: WasmStaticContext): JsExpression =
        JsStringLiteral("Dynamic operators are not supported in WASM target")
}

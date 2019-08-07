/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.backend.js.utils.IrNamer
import org.jetbrains.kotlin.ir.expressions.*

class IrElementToWasmExpressionTransformer : BaseIrElementToWasmNodeTransformer<WasmInstruction, IrNamer> {
    override fun visitVararg(expression: IrVararg, data: IrNamer): WasmInstruction {
        TODO("Support arrays")
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: IrNamer): WasmInstruction =
        body.expression.accept(this, data)

    override fun visitFunctionReference(expression: IrFunctionReference, data: IrNamer): WasmInstruction {
        TODO("?")
    }

    override fun <T> visitConst(expression: IrConst<T>, data: IrNamer): WasmInstruction {
        return when (val kind = expression.kind) {
            is IrConstKind.String,
            is IrConstKind.Null -> TODO()
            is IrConstKind.Boolean -> WasmI32Const(if (kind.valueOf(expression)) 1 else 0)
            is IrConstKind.Byte -> WasmI32Const(kind.valueOf(expression).toInt())
            is IrConstKind.Short -> WasmI32Const(kind.valueOf(expression).toInt())
            is IrConstKind.Int -> WasmI32Const(kind.valueOf(expression))
            is IrConstKind.Long -> WasmI64Const(kind.valueOf(expression))
            is IrConstKind.Char -> WasmI32Const(kind.valueOf(expression).toInt())
            is IrConstKind.Float -> WasmF32Const(kind.valueOf(expression))
            is IrConstKind.Double -> WasmF64Const(kind.valueOf(expression))
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: IrNamer): WasmInstruction {
        TODO("Implement kotlin.String")
    }

    override fun visitGetField(expression: IrGetField, data: IrNamer): WasmInstruction {
        TODO()
    }

    override fun visitGetValue(expression: IrGetValue, data: IrNamer): WasmInstruction =
        WasmGetLocal(data.getNameForValueDeclaration(expression.symbol.owner).ident)

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: IrNamer): WasmInstruction {
        TODO("IrGetObjectValue")
    }

    override fun visitSetField(expression: IrSetField, data: IrNamer): WasmInstruction {
        TODO()
//        val fieldName = data.getNameForField(expression.symbol.owner)
//        val dest = JsNameRef(fieldName, expression.receiver?.accept(this, data))
//        val source = expression.value.accept(this, data)
//        return jsAssignment(dest, source)
    }

    override fun visitSetVariable(expression: IrSetVariable, data: IrNamer): WasmInstruction {
        TODO()
//        val ref = JsNameRef(data.getNameForValueDeclaration(expression.symbol.owner))
//        val value = expression.value.accept(this, data)
//        return JsBinaryOperation(JsBinaryOperator.ASG, ref, value)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrNamer): WasmInstruction {
        TODO("IrDelegatingConstructorCall")
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: IrNamer): WasmInstruction {
        TODO("IrConstructorCall")
    }

    override fun visitCall(expression: IrCall, data: IrNamer): WasmInstruction {
        TODO()
//        return translateCall(expression, context, this)
    }

    override fun visitWhen(expression: IrWhen, data: IrNamer): WasmInstruction {
        TODO()
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrNamer): WasmInstruction {
        TODO("IrTypeOperatorCall")
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: IrNamer): WasmInstruction {
        TODO("IrGetEnumValue")
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: IrNamer): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: IrNamer): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")
}

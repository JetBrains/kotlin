/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.utils.getWasmInstructionAnnotation
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.dump

class ExpressionTransformer : BaseTransformer<WasmInstruction, WasmCodegenContext> {
    override fun visitVararg(expression: IrVararg, data: WasmCodegenContext): WasmInstruction {
        TODO("Support arrays")
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: WasmCodegenContext): WasmInstruction =
        body.expression.accept(this, data)

    override fun visitFunctionReference(expression: IrFunctionReference, data: WasmCodegenContext): WasmInstruction {
        TODO("?")
    }

    override fun <T> visitConst(expression: IrConst<T>, data: WasmCodegenContext): WasmInstruction {
        return when (val kind = expression.kind) {
            is IrConstKind.Null -> TODO()
            is IrConstKind.String -> {
                val value = kind.valueOf(expression)
                val index = data.stringLiterals.size
                data.stringLiterals.add(value)
                val funName = data.getGlobalName(data.backendContext.wasmSymbols.stringGetLiteral.owner)
                val operand = WasmI32Const(index)
                WasmCall(funName, listOf(operand))
            }
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

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: WasmCodegenContext): WasmInstruction {
        TODO("Implement kotlin.String")
    }

    override fun visitGetField(expression: IrGetField, data: WasmCodegenContext): WasmInstruction {
        val fieldName = data.getGlobalName(expression.symbol.owner)
        if (expression.receiver != null)
            TODO("Support member fields")

        return WasmGetGlobal(fieldName)
    }

    override fun visitGetValue(expression: IrGetValue, data: WasmCodegenContext): WasmInstruction =
        WasmGetLocal(data.getLocalName(expression.symbol.owner))

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: WasmCodegenContext): WasmInstruction {
        TODO("IrGetObjectValue")
    }

    override fun visitSetField(expression: IrSetField, data: WasmCodegenContext): WasmInstruction {
        val fieldName = data.getGlobalName(expression.symbol.owner)
        if (expression.receiver != null)
            TODO("Support member fields")

        val value = expression.value.accept(this, data)
        return WasmSetGlobal(fieldName, value)
    }

    override fun visitSetVariable(expression: IrSetVariable, data: WasmCodegenContext): WasmInstruction {
        val fieldName = data.getLocalName(expression.symbol.owner)
        val value = expression.value.accept(this, data)
        return WasmSetLocal(fieldName, value)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: WasmCodegenContext): WasmInstruction {
        TODO("IrConstructorCall")
    }

    override fun visitCall(expression: IrCall, data: WasmCodegenContext): WasmInstruction {
        val function = expression.symbol.owner.realOverrideTarget
        val valueArgs = (0 until expression.valueArgumentsCount).mapNotNull { expression.getValueArgument(it) }
        val irArguments = listOfNotNull(expression.dispatchReceiver, expression.extensionReceiver) + valueArgs
        val wasmArguments = irArguments.map { expressionToWasmInstruction(it, data) }

        val wasmInstruction = function.getWasmInstructionAnnotation()
        if (wasmInstruction != null) {
            if (wasmInstruction == "nop") {
                return wasmArguments.single()
            }
            return WasmSimpleInstruction(wasmInstruction, wasmArguments)
        }

        val name = data.getGlobalName(function)
        return WasmCall(name, wasmArguments)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: WasmCodegenContext): WasmInstruction {
        val wasmArgument = expressionToWasmInstruction(expression.argument, data)
        if (expression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
            return wasmArgument
        }
        TODO("IrTypeOperatorCall:\n ${expression.dump()}")
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: WasmCodegenContext): WasmInstruction {
        TODO("IrGetEnumValue")
    }

    override fun visitBlockBody(body: IrBlockBody, data: WasmCodegenContext): WasmInstruction {
        TODO()
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: WasmCodegenContext): WasmInstruction {
        val expressions = expression.statements.map { it.accept(this, data) }

        if (!expression.type.isUnit())
            return WasmBlock(expressions + listOf(WasmDrop(emptyList())))

        return WasmBlock(expressions)
    }

    override fun visitExpression(expression: IrExpression, data: WasmCodegenContext): WasmInstruction {
        return expressionToWasmInstruction(expression, data)
    }

    override fun visitBreak(jump: IrBreak, data: WasmCodegenContext): WasmInstruction {
        TODO()
    }

    override fun visitContinue(jump: IrContinue, data: WasmCodegenContext): WasmInstruction {
        TODO()
    }

    override fun visitReturn(expression: IrReturn, data: WasmCodegenContext): WasmInstruction {
        if (expression.value.type.isUnit()) return WasmReturn(emptyList())

        return WasmReturn(listOf(expressionToWasmInstruction(expression.value, data)))
    }

    override fun visitThrow(expression: IrThrow, data: WasmCodegenContext): WasmInstruction {
        TODO("IrThrow")
    }

    override fun visitVariable(declaration: IrVariable, data: WasmCodegenContext): WasmInstruction {
        val init = declaration.initializer ?: return WasmNop()
        val varName = data.getLocalName(declaration)
        return WasmSetLocal(varName, expressionToWasmInstruction(init, data))
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: WasmCodegenContext): WasmInstruction {
        TODO("IrDelegatingConstructorCall")
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: WasmCodegenContext): WasmInstruction {
        TODO("IrInstanceInitializerCall")
    }

    override fun visitTry(aTry: IrTry, data: WasmCodegenContext): WasmInstruction {
        TODO("IrTry")
    }

    override fun visitWhen(expression: IrWhen, data: WasmCodegenContext): WasmInstruction {
        return expression.branches.foldRight(null) { br: IrBranch, inst: WasmInstruction? ->
            val body = expressionToWasmInstruction(br.result, data)
            if (isElseBranch(br)) body
            else {
                val condition = expressionToWasmInstruction(br.condition, data)
                WasmIf(condition, WasmThen(body), inst?.let { WasmElse(inst) })
            }
        }!!
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: WasmCodegenContext): WasmInstruction {
        TODO("IrWhileLoop")
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: WasmCodegenContext): WasmInstruction {
        TODO("IrDoWhileLoop")
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: WasmCodegenContext): WasmInstruction {
        TODO("IrSyntheticBody")
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: WasmCodegenContext): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: WasmCodegenContext): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")
}

fun expressionToWasmInstruction(expression: IrExpression, context: WasmCodegenContext): WasmInstruction {
    return expression.accept(ExpressionTransformer(), context)
}
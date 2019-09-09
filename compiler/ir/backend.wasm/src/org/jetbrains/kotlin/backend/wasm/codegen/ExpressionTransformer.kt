/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.utils.getWasmInstructionAnnotation
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*

class ExpressionTransformer : BaseTransformer<WasmInstruction, WasmFunctionCodegenContext> {
    override fun visitVararg(expression: IrVararg, data: WasmFunctionCodegenContext): WasmInstruction {
        TODO("Support arrays")
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: WasmFunctionCodegenContext): WasmInstruction =
        body.expression.accept(this, data)

    override fun visitFunctionReference(expression: IrFunctionReference, data: WasmFunctionCodegenContext): WasmInstruction {
        TODO("?")
    }

    override fun <T> visitConst(expression: IrConst<T>, data: WasmFunctionCodegenContext): WasmInstruction {
        return when (val kind = expression.kind) {
            is IrConstKind.Null -> WasmRefNull
            is IrConstKind.String -> {
                val value = kind.valueOf(expression)
                val stringLiteralId = data.referenceStringLiteral(value)
                val funName = data.referenceFunction(data.backendContext.wasmSymbols.stringGetLiteral)
                val operand = WasmI32Symbol(stringLiteralId)
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

    override fun visitGetField(expression: IrGetField, data: WasmFunctionCodegenContext): WasmInstruction {
        val field = expression.symbol.owner
        val receiver = expression.receiver
        return when {
            receiver != null -> {
                val fieldClass = field.parentAsClass
                WasmStructGet(
                    data.referenceStructType(fieldClass.symbol), data.getStructFieldRef(field),
                    expressionToWasmInstruction(receiver, data)
                )
            }
            else -> {
                val fieldName = data.referenceGlobal(field.symbol)
                WasmGetGlobal(fieldName)
            }
        }
    }

    override fun visitGetValue(expression: IrGetValue, data: WasmFunctionCodegenContext): WasmInstruction =
        WasmGetLocal(data.referenceLocal(expression.symbol))

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: WasmFunctionCodegenContext): WasmInstruction {
        TODO("IrGetObjectValue ${expression.dump()}")
    }

    override fun visitSetField(expression: IrSetField, data: WasmFunctionCodegenContext): WasmInstruction {
        val field = expression.symbol.owner
        val receiver = expression.receiver
        val wasmValue = expressionToWasmInstruction(expression.value, data)
        return when {
            receiver != null -> {
                WasmStructSet(
                    structName = data.referenceStructType(field.parentAsClass.symbol),
                    fieldId = data.getStructFieldRef(field),
                    structRef = expressionToWasmInstruction(receiver, data),
                    value = wasmValue
                )
            }
            else -> WasmSetGlobal(data.referenceGlobal(expression.symbol), wasmValue)
        }
    }

    override fun visitSetVariable(expression: IrSetVariable, data: WasmFunctionCodegenContext): WasmInstruction {
        val fieldName = data.referenceLocal(expression.symbol)
        val value = expression.value.accept(this, data)
        return WasmSetLocal(fieldName, value)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: WasmFunctionCodegenContext): WasmInstruction {
        val klass = expression.symbol.owner.parentAsClass
        val structTypeName = data.referenceStructType(klass.symbol)
        val klassId = data.referenceClassId(klass.symbol)

        val fields = klass.allFields(data.backendContext.irBuiltIns)

        val initialValues = fields.mapIndexed { index, field ->
            if (index == 0)
                WasmI32Symbol(klassId)
            else
                defaultInitializerForType(data.transformType(field.type))
        }

        return WasmSimpleBlock(
            resultType = data.transformType(expression.type),
            instructions = listOf(
                WasmStructNew(structTypeName, initialValues),
                transformCall(expression, data)
            )
        )
    }

    override fun visitCall(expression: IrCall, data: WasmFunctionCodegenContext): WasmInstruction {
        return transformCall(expression, data)
    }

    private fun transformCall(
        call: IrFunctionAccessExpression,
        data: WasmFunctionCodegenContext,
        additionalArgument: WasmInstruction? = null
    ): WasmInstruction {
        val function = call.symbol.owner.realOverrideTarget
        val valueArgs = (0 until call.valueArgumentsCount).mapNotNull { call.getValueArgument(it) }
        val irArguments = listOfNotNull(call.dispatchReceiver, call.extensionReceiver) + valueArgs
        val wasmArguments = listOfNotNull(additionalArgument) + irArguments.map { expressionToWasmInstruction(it, data) }
        val symbols = data.backendContext.wasmSymbols

        if (function is IrSimpleFunction && function.isOverridable) {
            val klass = function.parentAsClass
            if (!klass.isInterface) {
                val classMetadata = data.getClassMetadata(klass.symbol)
                val vfSlot = classMetadata.virtualMethods.map { it.function }.indexOf(function)
                val tableIndex = WasmCall(data.referenceFunction(symbols.getVirtualMethodId), listOf(wasmArguments[0], WasmI32Const(vfSlot)))
                val functionType = data.referenceFunctionType(function.symbol)
                return WasmCallIndirect(functionType, wasmArguments + listOf(tableIndex))
            } else {
                return WasmUnreachable
                TODO("Support interface calls ${call.dump()}")
            }
        }

        val intrinsic = transformIntrinsic(call, function, wasmArguments, data)
        if (intrinsic != null)
            return intrinsic

        val name = data.referenceFunction(function.symbol)
        return WasmCall(name, wasmArguments)
    }

    private fun transformIntrinsic(
        call: IrFunctionAccessExpression,
        function: IrFunction,
        wasmArguments: List<WasmInstruction>,
        context: WasmFunctionCodegenContext
    ): WasmInstruction? {
        val symbols = context.backendContext.wasmSymbols

        val wasmInstruction = function.getWasmInstructionAnnotation()
        if (wasmInstruction != null) {
            if (wasmInstruction == "nop") {
                return wasmArguments.single()
            }
            return WasmSimpleInstruction(wasmInstruction, wasmArguments)
        }

        when (function.symbol) {
            symbols.wasmClassId -> {
                val klass = call.getTypeArgument(0)!!.getClass()
                    ?: error("No class given for wasmClassId intrinsic")
                assert(!klass.isInterface)
                return WasmI32Symbol(context.referenceClassId(klass.symbol))
            }

            symbols.wasmInterfaceId -> {
                val irInterface = call.getTypeArgument(0)!!.getClass()
                    ?: error("No interface given for wasmInterfaceId intrinsic")
                assert(irInterface.isInterface)
                return WasmI32Symbol(context.referenceInterfaceId(irInterface.symbol))
            }

            symbols.structNarrow -> {
                val fromType = call.getTypeArgument(0)!!
                val toType = call.getTypeArgument(1)!!
                return WasmStructNarrow(context.transformType(fromType), context.transformType(toType), wasmArguments[0])
            }
        }

        return null
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: WasmFunctionCodegenContext): WasmInstruction {
        error("TypeOperators should be lowered ${expression.dump()}")
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: WasmFunctionCodegenContext): WasmInstruction {
        TODO("IrGetEnumValue")
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: WasmFunctionCodegenContext): WasmInstruction {
        assert(expression.statements.isNotEmpty()) {
            "Empty blocks are not supported in expression context"
        }
        val watStatements = expression.statements.dropLast(1).flatMap { statementToWasmInstruction(it, data) }
        val watExpression = expressionToWasmInstruction(expression.statements.last() as IrExpression, data)
        if (watStatements.isEmpty())
            return watExpression
        return WasmSimpleBlock(watStatements + listOf(watExpression), resultType = data.transformResultType(expression.type))
    }

    override fun visitExpression(expression: IrExpression, data: WasmFunctionCodegenContext): WasmInstruction {
        return expressionToWasmInstruction(expression, data)
    }

    override fun visitBreak(jump: IrBreak, data: WasmFunctionCodegenContext): WasmInstruction {
        return WasmBr(data.referenceLoopLabel(jump.loop, LoopLabelType.BREAK))
    }

    override fun visitContinue(jump: IrContinue, data: WasmFunctionCodegenContext): WasmInstruction {
        return WasmBr(data.referenceLoopLabel(jump.loop, LoopLabelType.CONTINUE))
    }

    override fun visitReturn(expression: IrReturn, data: WasmFunctionCodegenContext): WasmInstruction {
        return WasmReturn(expressionToWasmInstruction(expression.value, data))
    }

    override fun visitThrow(expression: IrThrow, data: WasmFunctionCodegenContext): WasmInstruction {
        TODO("IrThrow")
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: WasmFunctionCodegenContext): WasmInstruction {
        val klass = data.currentIrFunction.parentAsClass

        // Don't delegate constructors of Any to Any.
        // TODO: Should this be handled before codegen?
        if (klass.defaultType.isAny()) return WasmNop

        return transformCall(expression, data, WasmGetLocal(data.referenceLocal(0)))
    }

    override fun visitTry(aTry: IrTry, data: WasmFunctionCodegenContext): WasmInstruction {
        TODO("IrTry")
    }

    override fun visitWhen(expression: IrWhen, data: WasmFunctionCodegenContext): WasmInstruction {
        val resultType = data.transformResultType(expression.type)
        return expression.branches.foldRight(null) { br: IrBranch, inst: WasmInstruction? ->
            val body = expressionToWasmInstruction(br.result, data)
            if (isElseBranch(br)) body
            else {
                val condition = expressionToWasmInstruction(br.condition, data)
                WasmIf(condition, resultType, WasmThen(listOf(body)), inst?.let { WasmElse(listOf(inst)) })
            }
        }!!
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: WasmFunctionCodegenContext): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: WasmFunctionCodegenContext): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")
}

fun expressionToWasmInstruction(expression: IrExpression, context: WasmFunctionCodegenContext): WasmInstruction {
    return expression.accept(ExpressionTransformer(), context)
}
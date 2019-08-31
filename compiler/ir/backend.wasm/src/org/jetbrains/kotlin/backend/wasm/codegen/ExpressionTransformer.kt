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
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*

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
            is IrConstKind.Null -> WasmRefNull
            is IrConstKind.String -> {
                val value = kind.valueOf(expression)
                val index = data.wasm.stringLiterals.size
                data.wasm.stringLiterals.add(value)
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

    override fun visitGetField(expression: IrGetField, data: WasmCodegenContext): WasmInstruction {
        val field = expression.symbol.owner
        val receiver = expression.receiver
        return when {
            receiver != null -> {
                val fieldClass = field.parentAsClass
                WasmStructGet(
                    data.getStructTypeName(fieldClass), data.getStructFieldRef(field),
                    expressionToWasmInstruction(receiver, data)
                )
            }
            else -> {
                val fieldName = data.getGlobalName(field)
                WasmGetGlobal(fieldName)
            }
        }
    }

    override fun visitGetValue(expression: IrGetValue, data: WasmCodegenContext): WasmInstruction =
        WasmGetLocal(data.getLocalName(expression.symbol.owner))

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: WasmCodegenContext): WasmInstruction {
        TODO("IrGetObjectValue ${expression.dump()}")
    }

    override fun visitSetField(expression: IrSetField, data: WasmCodegenContext): WasmInstruction {
        val field = expression.symbol.owner
        val receiver = expression.receiver
        val wasmValue = expressionToWasmInstruction(expression.value, data)
        return when {
            receiver != null -> {
                WasmStructSet(
                    structName = data.getStructTypeName(field.parentAsClass),
                    fieldId = data.getStructFieldRef(field),
                    structRef = expressionToWasmInstruction(receiver, data),
                    value = wasmValue
                )
            }
            else -> WasmSetGlobal(data.getGlobalName(expression.symbol.owner), wasmValue)
        }
    }

    override fun visitSetVariable(expression: IrSetVariable, data: WasmCodegenContext): WasmInstruction {
        val fieldName = data.getLocalName(expression.symbol.owner)
        val value = expression.value.accept(this, data)
        return WasmSetLocal(fieldName, value)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: WasmCodegenContext): WasmInstruction {
        val klass = expression.symbol.owner.parentAsClass
        val structTypeName = data.getStructTypeName(klass)
        val klassId = data.getClassId(klass)

        val fields = klass.allFields(data.backendContext.irBuiltIns)

        val initialValues = fields.mapIndexed { index, field ->
            if (index == 0)
                WasmI32Symbol(klassId)
            else
                defaultInitializerForType(data.transformType(field.type))
        }

        return WasmBlock(
            resultType = data.transformType(expression.type),
            instructions = listOf(
                WasmStructNew(structTypeName, initialValues),
                transformCall(expression, data)
            )
        )
    }

    override fun visitCall(expression: IrCall, data: WasmCodegenContext): WasmInstruction {
        return transformCall(expression, data)
    }

    private fun transformCall(
        call: IrFunctionAccessExpression,
        data: WasmCodegenContext,
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
                val classMetadata = data.getClassMetadata(klass)
                // TODO: Simplify
                val vfSlot = classMetadata.virtualMethods.map { it.function }.indexOf(function)
                // FIXME: Avoid double side effect of dispatch receiver
                val tableIndex =
                    WasmCall(data.getGlobalName(symbols.getVirtualMethodId.owner), listOf(wasmArguments[0], WasmI32Const(vfSlot)))
                val functionType = data.getFunctionTypeName(function)

                return WasmCallIdirect(functionType, wasmArguments + listOf(tableIndex))
            } else {
                return WasmUnreachable
                TODO("Support interface calls ${call.dump()}")
            }
        }

        val intrinsic = transformIntrinsic(call, function, wasmArguments, data)
        if (intrinsic != null)
            return intrinsic

        val name = data.getGlobalName(function)
        return WasmCall(name, wasmArguments)
    }

    private fun transformIntrinsic(
        call: IrFunctionAccessExpression,
        function: IrFunction,
        wasmArguments: List<WasmInstruction>,
        context: WasmCodegenContext
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
                return WasmI32Symbol(context.getClassId(klass))
            }

            symbols.wasmInterfaceId -> {
                val irInterface = call.getTypeArgument(0)!!.getClass()
                    ?: error("No interface given for wasmInterfaceId intrinsic")
                assert(irInterface.isInterface)
                return WasmI32Symbol(context.getInterfaceId(irInterface))
            }

            symbols.structNarrow -> {
                val fromType = call.getTypeArgument(0)!!
                val toType = call.getTypeArgument(1)!!
                return WasmStructNarrow(context.transformType(fromType), context.transformType(toType), wasmArguments[0])
            }
        }

        return null
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: WasmCodegenContext): WasmInstruction {
        error("TypeOperators should be lowered ${expression.dump()}")
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: WasmCodegenContext): WasmInstruction {
        TODO("IrGetEnumValue")
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: WasmCodegenContext): WasmInstruction {
        assert(expression.statements.isNotEmpty()) {
            "Empty blocks are not supported in expression context"
        }
        val watStatements = expression.statements.dropLast(1).flatMap { statementToWasmInstruction(it, data) }
        val watExpression = expressionToWasmInstruction(expression.statements.last() as IrExpression, data)
        if (watStatements.isEmpty())
            return watExpression
        return WasmBlock(watStatements + listOf(watExpression), resultType = data.resultType(expression.type))
    }

    override fun visitExpression(expression: IrExpression, data: WasmCodegenContext): WasmInstruction {
        return expressionToWasmInstruction(expression, data)
    }

    override fun visitBreak(jump: IrBreak, data: WasmCodegenContext): WasmInstruction {
        return WasmBr(data.getBreakLabelName(jump.loop))
    }

    override fun visitContinue(jump: IrContinue, data: WasmCodegenContext): WasmInstruction {
        return WasmBr(data.getContinueLabelName(jump.loop))
    }

    override fun visitReturn(expression: IrReturn, data: WasmCodegenContext): WasmInstruction {
        if (expression.value.type.isUnit()) return WasmReturn()

        return WasmReturn(expressionToWasmInstruction(expression.value, data))
    }

    override fun visitThrow(expression: IrThrow, data: WasmCodegenContext): WasmInstruction {
        TODO("IrThrow")
    }

    override fun visitVariable(declaration: IrVariable, data: WasmCodegenContext): WasmInstruction {
        val init = declaration.initializer ?: return WasmNop
        val varName = data.getLocalName(declaration)
        return WasmSetLocal(varName, expressionToWasmInstruction(init, data))
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: WasmCodegenContext): WasmInstruction {
        val klass = data.currentClass!!
        if (klass.defaultType.isAny()) return WasmNop
        return transformCall(expression, data, WasmGetLocal(0))
    }

    override fun visitTry(aTry: IrTry, data: WasmCodegenContext): WasmInstruction {
        TODO("IrTry")
    }

    override fun visitWhen(expression: IrWhen, data: WasmCodegenContext): WasmInstruction {
        val resultType = data.resultType(expression.type)
        return expression.branches.foldRight(null) { br: IrBranch, inst: WasmInstruction? ->
            val body = expressionToWasmInstruction(br.result, data)
            if (isElseBranch(br)) body
            else {
                val condition = expressionToWasmInstruction(br.condition, data)
                WasmIf(condition, resultType, WasmThen(listOf(body)), inst?.let { WasmElse(listOf(inst)) })
            }
        }!!
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: WasmCodegenContext): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: WasmCodegenContext): WasmInstruction =
        error("Dynamic operators are not supported for WASM target")
}

fun expressionToWasmInstruction(expression: IrExpression, context: WasmCodegenContext): WasmInstruction {
    return expression.accept(ExpressionTransformer(), context)
}
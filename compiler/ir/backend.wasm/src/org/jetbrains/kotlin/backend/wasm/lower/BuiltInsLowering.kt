/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.isEqualsInheritedFromAny
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

class BuiltInsLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val irBuiltins = context.irBuiltIns
    private val symbols = context.wasmSymbols

    private fun IrType.findEqualsMethod(): IrSimpleFunction {
        val klass = getClass() ?: irBuiltins.anyClass.owner
        return klass.functions.single { it.isEqualsInheritedFromAny() }
    }

    fun transformCall(
        call: IrCall,
        builder: DeclarationIrBuilder
    ): IrExpression {
        when (val symbol = call.symbol) {
            irBuiltins.ieee754equalsFunByOperandType[irBuiltins.floatClass] -> {
                if (call.getValueArgument(0)!!.type.isNullable() || call.getValueArgument(1)!!.type.isNullable()) {
                    return irCall(call, symbols.nullableFloatIeee754Equals)
                }
                return irCall(call, symbols.floatEqualityFunctions.getValue(irBuiltins.floatType))
            }
            irBuiltins.ieee754equalsFunByOperandType[irBuiltins.doubleClass] -> {
                if (call.getValueArgument(0)!!.type.isNullable() || call.getValueArgument(1)!!.type.isNullable()) {
                    return irCall(call, symbols.nullableDoubleIeee754Equals)
                }
                return irCall(call, symbols.floatEqualityFunctions.getValue(irBuiltins.doubleType))
            }
            irBuiltins.eqeqSymbol -> {
                val lhs = call.getValueArgument(0)!!
                val rhs = call.getValueArgument(1)!!
                val lhsType = lhs.type
                val rhsType = rhs.type
                if (lhsType == rhsType) {
                    val newSymbol = symbols.equalityFunctions[lhsType]
                    if (newSymbol != null) {
                        return irCall(call, newSymbol)
                    }
                }
                if (lhs.isNullConst()) {
                    return builder.irCall(symbols.refIsNull).apply { putValueArgument(0, rhs) }
                }
                if (rhs.isNullConst()) {
                    return builder.irCall(symbols.refIsNull).apply { putValueArgument(0, lhs) }
                }
                if (!lhsType.isNullable()) {
                    return irCall(call, lhsType.findEqualsMethod().symbol, argumentsAsReceivers = true)
                }
                return irCall(call, symbols.nullableEquals)
            }

            irBuiltins.eqeqeqSymbol -> {
                val type = call.getValueArgument(0)!!.type
                val newSymbol = symbols.equalityFunctions[type] ?: symbols.floatEqualityFunctions[type] ?: symbols.refEq
                return irCall(call, newSymbol)
            }

            irBuiltins.checkNotNullSymbol -> {
                return irCall(call, symbols.ensureNotNull).also {
                    it.putTypeArgument(0, call.type)
                }
            }
            in symbols.comparisonBuiltInsToWasmIntrinsics.keys -> {
                val newSymbol = symbols.comparisonBuiltInsToWasmIntrinsics[symbol]!!
                return irCall(call, newSymbol)
            }

            // TODO: Implement
            irBuiltins.noWhenBranchMatchedExceptionSymbol ->
                return builder.irCall(symbols.wasmUnreachable, irBuiltins.nothingType)

            // TODO: Implement
            irBuiltins.illegalArgumentExceptionSymbol ->
                return builder.irCall(symbols.wasmUnreachable, irBuiltins.nothingType)

            irBuiltins.dataClassArrayMemberHashCodeSymbol -> {
                // TODO: Implement
                return builder.irComposite {
                    +call.getValueArgument(0)!!
                    +irInt(7777)
                }
            }
            irBuiltins.dataClassArrayMemberToStringSymbol -> {
                // TODO: Implement
                return builder.irCall(symbols.anyNtoString).apply {
                    putValueArgument(0, call.getValueArgument(0))
                }
            }
        }

        val nativeInvoke = isNativeInvoke(call)
        if (nativeInvoke != null) {
            return irCall(call, symbols.functionNInvokeMethods[nativeInvoke])
        }

        return call
    }

    private fun isNativeInvoke(call: IrCall): Int? {
        val simpleFunction = call.symbol.owner as? IrSimpleFunction ?: return null
        val receiverType = simpleFunction.dispatchReceiverParameter?.type ?: return null
        if (simpleFunction.isSuspend) return null
        if (simpleFunction.name == OperatorNameConventions.INVOKE && receiverType.isFunction()) {
            return simpleFunction.valueParameters.size
        }
        return null
    }

    override fun lower(irFile: IrFile) {
        val builder = context.createIrBuilder(irFile.symbol)
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                val newExpression = transformCall(expression, builder)
                newExpression.transformChildrenVoid(this)
                return newExpression
            }
        })
    }
}

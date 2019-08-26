/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isEqualsInheritedFromAny
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class BuiltInsLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val irBuiltins = context.irBuiltIns
    private val symbols = context.wasmSymbols

    private fun IrType.findEqualsMethod(): IrSimpleFunction? {
        val klass = getClass() ?: irBuiltins.anyClass.owner
        return klass.declarations
            .filterIsInstance<IrSimpleFunction>()
            // TODO: don't generate bridges for equals method and use (.single { })
            .first { it.isEqualsInheritedFromAny() }
    }

    fun transformCall(call: IrCall): IrExpression {
        when (val symbol = call.symbol) {
            // TODO: Clean this up
            in irBuiltins.ieee754equalsFunByOperandType.values -> {
                val type = call.symbol.owner.valueParameters[0]!!.type.makeNotNull()
                val newSymbol = when {
                    type.isDouble() -> symbols.nullableDoubleIeee754Equals
                    type.isFloat() -> symbols.nullableFloatIeee754Equals
                    else -> error("Unexpected IEEE operation type: ${type.render()}")
                }
                return irCall(call, newSymbol)
            }
            irBuiltins.eqeqSymbol, irBuiltins.eqeqeqSymbol, in irBuiltins.ieee754equalsFunByOperandType.values -> {
                val type = call.getValueArgument(0)!!.type
                val newSymbol = symbols.equalityFunctions[type]
                if (newSymbol == null) {
                    if (symbol == irBuiltins.eqeqeqSymbol) {
                        return irCall(call, symbols.refEq)
                    }
                    if (type.isNullable()) {
                        return irCall(call, symbols.nullableEquals)
                    }
                    val equalsMethod = type.findEqualsMethod()?.symbol
                        ?: error("Unsupported equality operator with type: ${type.render()}")
                    return irCall(call, equalsMethod, argumentsAsReceivers = true)
                } else {
                    val type2 = call.getValueArgument(1)!!.type
                    if (type2.isNullable()) {
                        return irCall(call, symbols.nullableEquals)
                    }
                }
                return irCall(call, newSymbol)
            }

            irBuiltins.checkNotNullSymbol -> {
                return irCall(call, symbols.ensureNotNull).also {
                    it.putTypeArgument(0, call.type)
                }
            }
            in symbols.irBuiltInsToWasmIntrinsics.keys -> {
                val newSymbol = symbols.irBuiltInsToWasmIntrinsics[symbol]!!
                return irCall(call, newSymbol)
            }
            irBuiltins.noWhenBranchMatchedExceptionSymbol,
            irBuiltins.illegalArgumentExceptionSymbol ->
                return JsIrBuilder.buildCall(context.wasmSymbols.unreachable, context.irBuiltIns.nothingType)

            irBuiltins.andandSymbol -> TODO("irBuiltins.andandSymbol")
            irBuiltins.ororSymbol -> TODO("irBuiltins.ororSymbol")
            irBuiltins.dataClassArrayMemberHashCodeSymbol -> TODO("irBuiltins.dataClassArrayMemberHashCodeSymbol")
            irBuiltins.dataClassArrayMemberToStringSymbol -> TODO("irBuiltins.dataClassArrayMemberToStringSymbol")
        }
        return call
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val newExpression = transformCall(expression)
                newExpression.transformChildrenVoid(this)
                return newExpression
            }
        })
    }
}

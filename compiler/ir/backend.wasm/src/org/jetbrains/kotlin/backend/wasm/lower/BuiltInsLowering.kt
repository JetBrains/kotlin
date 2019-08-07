/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class BuiltInsLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val irBuiltins = context.irBuiltIns
    private val symbols = context.wasmSymbols

    fun transformCall(call: IrCall): IrExpression {
        when (val symbol = call.symbol) {
            irBuiltins.eqeqSymbol, irBuiltins.eqeqeqSymbol, in irBuiltins.ieee754equalsFunByOperandType.values -> {
                val type = call.getValueArgument(0)!!.type
                val newSymbol = symbols.equalityFunctions[type]
                    ?: error("Unsupported equality operator with type: ${type.render()}")
                return irCall(call, newSymbol)
            }
            in symbols.irBuiltInsToWasmIntrinsics.keys -> {
                val newSymbol = symbols.irBuiltInsToWasmIntrinsics[symbol]!!
                return irCall(call, newSymbol)
            }
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

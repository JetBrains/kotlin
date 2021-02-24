/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Replace throw expressions with a runtime function call.
 * TODO: Remove when full-blown exception handling is implemented
 */
internal class WasmThrowDebugLowering(
    private val context: WasmBackendContext
) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitThrow(expression: IrThrow): IrExpression {
        expression.transformChildrenVoid(this)
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol)

        return builder.irCall(context.wasmSymbols.wasmThrow).apply {
            this.putValueArgument(0, expression.value)
        }
    }
}
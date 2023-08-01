/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.inline.DefaultInlineFunctionResolver
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

class WasmInlineFunctionResolver(context: WasmBackendContext) : DefaultInlineFunctionResolver(context) {
    private val enumEntriesIntrinsic = context.wasmSymbols.enumEntriesIntrinsic

    override fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol): Boolean {
        // TODO: After the expect fun enumEntriesIntrinsic become non-inline function, the code will be removed
        return symbol == enumEntriesIntrinsic || super.shouldExcludeFunctionFromInlining(symbol)
    }
}
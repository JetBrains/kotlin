/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.irAnnotation
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction

/**
 * Mark functions matched by [matcher] with @JsExport and @WasmExport annotations, respectively for wasm-js and wasm-wasi.
 */
fun markFunctionToExport(context: WasmBackendContext, irFile: IrFile, matcher: IrFunction.() -> Boolean) {
    val exportConstructor = when (context.isWasmJsTarget) {
        true -> context.wasmSymbols.jsRelatedSymbols.jsExportConstructor
        else -> context.wasmSymbols.wasmExportConstructor
    }

    irFile.declarations.find { it is IrFunction && it.matcher() }?.let {
        val builder = context.createIrBuilder(irFile.symbol)
        it.annotations += builder.irAnnotation(exportConstructor, typeArguments = emptyList())
    }
}

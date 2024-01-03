/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.WasmTarget
import org.jetbrains.kotlin.name.FqName

/**
 * Mark declarations from [exportedFqNames] with @JsExport annotation
 */
fun markExportedDeclarations(context: WasmBackendContext, irFile: IrFile, exportedFqNames: Set<FqName>) {
    val exportConstructor = when (context.isWasmJsTarget) {
        true -> context.wasmSymbols.jsRelatedSymbols.jsExportConstructor
        else -> context.wasmSymbols.wasmExportConstructor
    }

    for (declaration in irFile.declarations) {
        if (declaration is IrFunction && declaration.fqNameWhenAvailable in exportedFqNames) {
            val builder = context.createIrBuilder(irFile.symbol)
            declaration.annotations +=
                builder.irCallConstructor(exportConstructor, typeArguments = emptyList())
        }
    }
}
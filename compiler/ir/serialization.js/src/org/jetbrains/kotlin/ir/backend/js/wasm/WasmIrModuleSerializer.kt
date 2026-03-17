/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrModuleSerializer
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsGlobalDeclarationTable

class WasmIrModuleSerializer(
    settings: IrSerializationSettings,
    diagnosticReporter: IrDiagnosticReporter,
    irBuiltIns: IrBuiltIns,
    private val wasmIrFileMetadataFactory: WasmIrFileMetadataFactory = WasmIrFileEmptyMetadataFactory,
) : IrModuleSerializer<WasmIrFileSerializer>(settings, diagnosticReporter) {

    override val globalDeclarationTable = JsGlobalDeclarationTable(irBuiltIns)

    override fun createFileSerializer(settings: IrSerializationSettings): WasmIrFileSerializer =
        WasmIrFileSerializer(settings, DeclarationTable.Default(globalDeclarationTable), wasmIrFileMetadataFactory)
}

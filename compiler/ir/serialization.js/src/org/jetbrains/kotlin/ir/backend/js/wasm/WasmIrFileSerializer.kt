/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.isJsExportDeclaration
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.isJsExportIgnoreDeclaration
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile

fun interface WasmIrFileMetadataFactory {
    fun createWasmIrFileMetadata(irFile: IrFile): WasmIrFileMetadata
}

object WasmIrFileEmptyMetadataFactory : WasmIrFileMetadataFactory {
    override fun createWasmIrFileMetadata(irFile: IrFile) = WasmIrFileMetadata(emptyMap())
}

class WasmIrFileSerializer(
    settings: IrSerializationSettings,
    declarationTable: DeclarationTable.Default,
    private val wasmIrFileMetadataFactory: WasmIrFileMetadataFactory
) : IrFileSerializer(settings, declarationTable) {

    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer) =
        node.isJsExportDeclaration() || node.isWasmExportDeclaration()

    override fun backendSpecificExplicitRootExclusion(node: IrAnnotationContainer) =
        node.isJsExportIgnoreDeclaration()

    override fun backendSpecificMetadata(irFile: IrFile) = wasmIrFileMetadataFactory.createWasmIrFileMetadata(irFile)
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ast.WasmExport
import org.jetbrains.kotlin.backend.wasm.ast.WasmExportKind
import org.jetbrains.kotlin.backend.wasm.ast.WasmModule
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.utils.addIfNotNull

class IrModuleToWasm(private val backendContext: WasmBackendContext) {
    fun generateModule(module: IrModuleFragment): WasmModule {
        val nameTable = IrNamerImpl(NameTables(module.files))
        val context = WasmStaticContext(nameTable)
        val irDeclarations = module.files.flatMap { it.declarations }
        val wasmFields = irDeclarations.flatMap { it.accept(IrDeclarationToWasmTransformer(), context) }
        val exports = generateExports(module, nameTable)
        return WasmModule(wasmFields + exports)
    }

    private fun generateExports(module: IrModuleFragment, namer: IrNamer): List<WasmExport> {
        val exports = mutableListOf<WasmExport>()
        for (file in module.files) {
            for (declaration in file.declarations) {
                exports.addIfNotNull(generateExport(declaration, namer))
            }
        }
        return exports
    }

    private fun generateExport(
        declaration: IrDeclaration,
        namer: IrNamer
    ): WasmExport? {
        if (declaration !is IrDeclarationWithVisibility ||
            declaration !is IrDeclarationWithName ||
            declaration !is IrSimpleFunction ||
            declaration.visibility != Visibilities.PUBLIC
        ) {
            return null
        }

        if (!declaration.isExported())
            return null

        val name = namer.getNameForStaticFunction(declaration).ident
        val exportName = sanitizeName(declaration.name.identifier)

        return WasmExport(
            wasmName = name,
            exportedName = exportName,
            kind = WasmExportKind.FUNCTION
        )
    }

    private fun IrFunction.isExported(): Boolean {
        if (fqNameWhenAvailable in backendContext.additionalExportedDeclarations)
            return true
        if (isJsExport())
            return true
        return false
    }
}
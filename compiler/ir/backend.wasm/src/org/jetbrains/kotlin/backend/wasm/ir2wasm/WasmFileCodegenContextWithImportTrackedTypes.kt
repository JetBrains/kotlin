/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

class WasmFileCodegenContextWithImportTrackedTypes(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
    moduleName: String,
    moduleReferencedDeclarations: ModuleReferencedDeclarations,
    private val moduleReferencedTypes: ModuleReferencedTypes,
) : WasmFileCodegenContextWithImport(wasmFileFragment, idSignatureRetriever, moduleName, moduleReferencedDeclarations) {
    override fun needToBeDefinedGcType(declaration: IrClassSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        return signature in moduleReferencedTypes.referencedGcTypes
    }

    override fun needToBeDefinedFunctionType(declaration: IrFunctionSymbol): Boolean {
        val signature = idSignatureRetriever.declarationSignature(declaration.owner)
        return signature in moduleReferencedTypes.referencedFunctionTypes
    }
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class WasmModuleFragmentGenerator(
    private val backendContext: WasmBackendContext,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
    private val idSignatureRetriever: IdSignatureRetriever,
    private val allowIncompleteImplementations: Boolean,
) {
    fun generateModule(irModuleFragment: IrModuleFragment): List<WasmCompiledFileFragment> {
        val wasmCompiledModuleFragments = mutableListOf<WasmCompiledFileFragment>()
        for (irFile in irModuleFragment.files) {
            val wasmFileFragment = WasmCompiledFileFragment()
            val wasmFileCodegenContext = WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever)
            val wasmModuleTypeTransformer = WasmModuleTypeTransformer(backendContext, wasmFileCodegenContext)

            val generator = DeclarationGenerator(
                backendContext,
                wasmFileCodegenContext,
                wasmModuleTypeTransformer,
                wasmModuleMetadataCache,
                allowIncompleteImplementations,
            )
            for (irDeclaration in irFile.declarations) {
                irDeclaration.acceptVoid(generator)
            }
            wasmCompiledModuleFragments.add(wasmFileFragment)
        }
        return wasmCompiledModuleFragments
    }
}

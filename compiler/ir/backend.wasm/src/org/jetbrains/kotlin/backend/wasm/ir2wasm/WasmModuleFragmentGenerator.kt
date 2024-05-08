/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class WasmModuleFragmentGenerator(
    backendContext: WasmBackendContext,
    wasmModuleFragment: WasmCompiledModuleFragment,
    idSignatureRetriever: IdSignatureRetriever,
    allowIncompleteImplementations: Boolean,
) {
    val declarationGenerator =
        DeclarationGenerator(
            WasmModuleCodegenContext(
                backendContext,
                idSignatureRetriever,
                wasmModuleFragment,
            ),
            allowIncompleteImplementations,
        )

    fun generateModule(irModuleFragment: IrModuleFragment) {
        acceptVisitor(irModuleFragment, declarationGenerator)
    }

    private fun acceptVisitor(irModuleFragment: IrModuleFragment, visitor: IrElementVisitorVoid) {
        for (irFile in irModuleFragment.files) {
            for (irDeclaration in irFile.declarations) {
                irDeclaration.acceptVoid(visitor)
            }
        }
    }
}

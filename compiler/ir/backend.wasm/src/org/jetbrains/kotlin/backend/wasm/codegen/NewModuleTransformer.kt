/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class WasmCodeGenerator(
    private val backendContext: WasmBackendContext,
    private val wasmModuleFragment: WasmCompiledModuleFragment
) {
    private val declarationGenerator = DeclarationGenerator(backendContext, wasmModuleFragment)

    fun generateModule(irModuleFragment: IrModuleFragment) {
        for (irFile in irModuleFragment.files) {
            generatePackageFragment(irFile)
        }
    }

    fun generatePackageFragment(irPackageFragment: IrPackageFragment) {
        for (irDeclaration in irPackageFragment.declarations) {
            generateDeclaration(irDeclaration)
        }
    }

    fun generateDeclaration(irDeclaration: IrDeclaration) {
        irDeclaration.acceptVoid(declarationGenerator)
    }
}

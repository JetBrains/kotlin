/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class WasmCodeGenerator(
    backendContext: WasmBackendContext,
    wasmModuleFragment: WasmCompiledModuleFragment
) {
    private val declarationGenerator =
        DeclarationGenerator(
            WasmModuleCodegenContextImpl(
                backendContext,
                wasmModuleFragment
            )
        )

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

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.DisjointUnions
import org.jetbrains.kotlin.backend.wasm.utils.getWasmArrayAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.isAbstractOrSealed
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class WasmModuleFragmentGenerator(
    backendContext: WasmBackendContext,
    wasmModuleFragment: WasmCompiledModuleFragment,
    allowIncompleteImplementations: Boolean,
) {
    private val hierarchyDisjointUnions = DisjointUnions<IrClassSymbol>()

    private val declarationGenerator =
        DeclarationGenerator(
            WasmModuleCodegenContext(
                backendContext,
                wasmModuleFragment,
            ),
            allowIncompleteImplementations,
            hierarchyDisjointUnions,
        )

    private val interfaceCollector = object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) { }

        override fun visitClass(declaration: IrClass) {
            if (declaration.isExternal) return
            if (declaration.getWasmArrayAnnotation() != null) return
            if (declaration.isInterface) return
            if (declaration.isAbstractOrSealed) return

            val classMetadata = declarationGenerator.context.getClassMetadata(declaration.symbol)
            if (classMetadata.interfaces.isNotEmpty()) {
                hierarchyDisjointUnions.addUnion(classMetadata.interfaces.map { it.symbol })
            }
        }
    }

    fun collectInterfaceTables(irModuleFragment: IrModuleFragment) {
        acceptVisitor(irModuleFragment, interfaceCollector)
        hierarchyDisjointUnions.compress()
    }

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

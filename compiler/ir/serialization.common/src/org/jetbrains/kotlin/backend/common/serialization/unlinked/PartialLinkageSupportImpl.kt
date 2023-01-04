/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.allUnbound

internal class PartialLinkageSupportImpl(builtIns: IrBuiltIns, messageLogger: IrMessageLogger) : PartialLinkageSupport {
    private val stubGenerator = MissingDeclarationStubGenerator(builtIns)
    private val classifierExplorer = ClassifierExplorer(builtIns, stubGenerator)
    private val patcher = PartiallyLinkedIrTreePatcher(builtIns, classifierExplorer, stubGenerator, messageLogger)

    override val partialLinkageEnabled get() = true

    override fun exploreClassifiers(fakeOverrideBuilder: FakeOverrideBuilder) {
        val entries = fakeOverrideBuilder.fakeOverrideCandidates
        if (entries.isEmpty()) return

        val toExclude = buildSet {
            for (clazz in entries.keys) {
                if (classifierExplorer.exploreSymbol(clazz.symbol) != null) {
                    this += clazz
                }
            }
        }

        entries -= toExclude
    }

    override fun exploreClassifiersInInlineLazyIrFunction(function: IrFunction) {
        classifierExplorer.exploreIrElement(function)
    }

    override fun generateStubsAndPatchUsages(symbolTable: SymbolTable, roots: () -> Sequence<IrModuleFragment>) {
        generateStubsAndPatchUsagesInternal(symbolTable) { patcher.patchModuleFragments(roots()) }
    }

    override fun generateStubsAndPatchUsages(symbolTable: SymbolTable, root: IrDeclaration) {
        generateStubsAndPatchUsagesInternal(symbolTable) { patcher.patchDeclarations(listOf(root)) }
    }

    private fun generateStubsAndPatchUsagesInternal(symbolTable: SymbolTable, patchIrTree: () -> Unit) {
        // Generate stubs.
        for (symbol in symbolTable.allUnbound) {
            stubGenerator.getDeclaration(symbol)
        }

        // Patch the IR tree.
        patchIrTree()

        // Patch the stubs which were not patched yet.
        patcher.patchDeclarations(stubGenerator.grabDeclarationsToPatch())
    }
}

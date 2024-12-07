/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.backend.common.linkage.issues.PartialLinkageErrorsLogged
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageLogger
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable

fun createPartialLinkageSupportForLinker(
    partialLinkageConfig: PartialLinkageConfig,
    builtIns: IrBuiltIns,
    messageCollector: MessageCollector
): PartialLinkageSupportForLinker = if (partialLinkageConfig.isEnabled)
    PartialLinkageSupportForLinkerImpl(builtIns, PartialLinkageLogger(messageCollector, partialLinkageConfig.logLevel))
else
    PartialLinkageSupportForLinker.DISABLED

internal class PartialLinkageSupportForLinkerImpl(
    builtIns: IrBuiltIns,
    private val logger: PartialLinkageLogger
) : PartialLinkageSupportForLinker {
    private val stubGenerator = MissingDeclarationStubGenerator(builtIns)
    private val classifierExplorer = ClassifierExplorer(builtIns, stubGenerator)
    private val patcher = PartiallyLinkedIrTreePatcher(builtIns, classifierExplorer, stubGenerator, logger)

    override val isEnabled get() = true

    override fun shouldBeSkipped(declaration: IrDeclaration) = patcher.shouldBeSkipped(declaration)

    override fun exploreClassifiers(fakeOverrideBuilder: IrLinkerFakeOverrideProvider) {
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

    override fun collectAllStubbedSymbols(): Set<IrSymbol> {
        return stubGenerator.allStubbedSymbols
    }

    private fun generateStubsAndPatchUsagesInternal(symbolTable: SymbolTable, patchIrTree: () -> Unit) {
        // Generate stubs.
        for (symbol in symbolTable.descriptorExtension.allUnboundSymbols) {
            stubGenerator.getDeclaration(symbol)
        }

        // Patch the IR tree.
        patchIrTree()

        // Patch the stubs which were not patched yet.
        patcher.patchDeclarations(stubGenerator.grabDeclarationsToPatch())

        // Make sure that there are no linkage issues that have been reported with the 'error' severity.
        // If there are, abort the current compilation.
        if (logger.logLevel == PartialLinkageLogLevel.ERROR && patcher.linkageIssuesLogged > 0)
            PartialLinkageErrorsLogged.raiseIssue(logger.messageCollector)
    }
}

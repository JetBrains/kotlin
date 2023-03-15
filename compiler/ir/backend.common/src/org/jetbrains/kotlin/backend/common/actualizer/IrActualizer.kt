/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.FqName

data class IrActualizationResult(val actualizedExpectDeclarations: List<IrDeclaration>)

object IrActualizer {
    fun actualize(
        mainFragment: IrModuleFragment,
        dependentFragments: List<IrModuleFragment>,
        diagnosticReporter: DiagnosticReporter,
        languageVersionSettings: LanguageVersionSettings
    ): IrActualizationResult {
        val ktDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, languageVersionSettings)
        val (expectActualMap, expectActualTypeAliasMap) = ExpectActualCollector(mainFragment, dependentFragments, ktDiagnosticReporter).collect()
        FunctionDefaultParametersActualizer(expectActualMap).actualize()
        val removedExpectDeclarationMetadata = removeExpectDeclarations(dependentFragments, expectActualMap)
        addMissingFakeOverrides(expectActualMap, dependentFragments, expectActualTypeAliasMap, ktDiagnosticReporter)
        linkExpectToActual(expectActualMap, dependentFragments)
        mergeIrFragments(mainFragment, dependentFragments)
        return IrActualizationResult(removedExpectDeclarationMetadata)
    }

    private fun removeExpectDeclarations(dependentFragments: List<IrModuleFragment>, expectActualMap: Map<IrSymbol, IrSymbol>): List<IrDeclaration> {
        val removedDeclarationMetadata = mutableListOf<IrDeclaration>()
        for (fragment in dependentFragments) {
            for (file in fragment.files) {
                file.declarations.removeIf {
                    if (shouldRemoveExpectDeclaration(it, expectActualMap)) {
                        removedDeclarationMetadata.add(it)
                        true
                    } else {
                        false
                    }
                }
            }
        }
        return removedDeclarationMetadata
    }

    private fun shouldRemoveExpectDeclaration(irDeclaration: IrDeclaration, expectActualMap: Map<IrSymbol, IrSymbol>): Boolean {
        return when (irDeclaration) {
            is IrClass -> irDeclaration.isExpect && (!irDeclaration.containsOptionalExpectation() || expectActualMap.containsKey(irDeclaration.symbol))
            is IrProperty -> irDeclaration.isExpect
            is IrFunction -> irDeclaration.isExpect
            else -> false
        }
    }

    private fun addMissingFakeOverrides(
        expectActualMap: Map<IrSymbol, IrSymbol>,
        dependentFragments: List<IrModuleFragment>,
        expectActualTypeAliasMap: Map<FqName, FqName>,
        diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext
    ) {
        MissingFakeOverridesAdder(
            expectActualMap,
            expectActualTypeAliasMap,
            diagnosticsReporter
        ).apply { dependentFragments.forEach { visitModuleFragment(it) } }
    }

    private fun linkExpectToActual(expectActualMap: Map<IrSymbol, IrSymbol>, dependentFragments: List<IrModuleFragment>) {
        ExpectActualLinker(expectActualMap).apply { dependentFragments.forEach { actualize(it) } }
    }

    private fun mergeIrFragments(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>) {
        mainFragment.files.addAll(0, dependentFragments.flatMap { it.files })
    }
}
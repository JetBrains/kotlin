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
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper

data class IrActualizedResult(val actualizedExpectDeclarations: List<IrDeclaration>)

object IrActualizer {
    fun actualize(
        mainFragment: IrModuleFragment,
        dependentFragments: List<IrModuleFragment>,
        diagnosticReporter: DiagnosticReporter,
        languageVersionSettings: LanguageVersionSettings
    ): IrActualizedResult {
        val ktDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, languageVersionSettings)

        val (expectActualMap, expectActualTypeAliasMap) = ExpectActualCollector(
            mainFragment,
            dependentFragments,
            ktDiagnosticReporter
        ).collect()

        val removedExpectDeclarations = removeExpectDeclarations(dependentFragments, expectActualMap)

        val symbolRemapper = ActualizerSymbolRemapper(expectActualMap)
        val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)
        FunctionDefaultParametersActualizer(symbolRemapper, typeRemapper, expectActualMap).actualize()

        MissingFakeOverridesAdder(
            expectActualMap,
            expectActualTypeAliasMap,
            ktDiagnosticReporter
        ).apply { dependentFragments.forEach { visitModuleFragment(it) } }

        val actualizerVisitor = ActualizerVisitor(symbolRemapper, typeRemapper)
        dependentFragments.forEach { it.transform(actualizerVisitor, null) }

        mergeIrFragments(mainFragment, dependentFragments)

        return IrActualizedResult(removedExpectDeclarations)
    }

    private fun removeExpectDeclarations(dependentFragments: List<IrModuleFragment>, expectActualMap: Map<IrSymbol, IrSymbol>): List<IrDeclaration> {
        val removedExpectDeclarations = mutableListOf<IrDeclaration>()
        for (fragment in dependentFragments) {
            for (file in fragment.files) {
                file.declarations.removeIf {
                    if (shouldRemoveExpectDeclaration(it, expectActualMap)) {
                        removedExpectDeclarations.add(it)
                        true
                    } else {
                        false
                    }
                }
            }
        }
        return removedExpectDeclarations
    }

    private fun shouldRemoveExpectDeclaration(irDeclaration: IrDeclaration, expectActualMap: Map<IrSymbol, IrSymbol>): Boolean {
        return when (irDeclaration) {
            is IrClass -> irDeclaration.isExpect && (!irDeclaration.containsOptionalExpectation() || expectActualMap.containsKey(irDeclaration.symbol))
            is IrProperty -> irDeclaration.isExpect
            is IrFunction -> irDeclaration.isExpect
            else -> false
        }
    }

    private fun mergeIrFragments(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>) {
        mainFragment.files.addAll(0, dependentFragments.flatMap { it.files })
    }
}
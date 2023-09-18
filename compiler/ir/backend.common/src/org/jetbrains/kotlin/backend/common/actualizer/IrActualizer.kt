/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.actualizer.checker.IrExpectActualCheckers
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.*

data class IrActualizedResult(val actualizedExpectDeclarations: List<IrDeclaration>)

object IrActualizer {
    fun actualize(
        mainFragment: IrModuleFragment,
        dependentFragments: List<IrModuleFragment>,
        diagnosticReporter: DiagnosticReporter,
        typeSystemContext: IrTypeSystemContext,
        languageVersionSettings: LanguageVersionSettings,
        symbolTable: SymbolTable,
        fakeOverrideBuilder: IrFakeOverrideBuilder,
        useIrFakeOverrideBuilder: Boolean,
        expectActualTracker: ExpectActualTracker?
    ): IrActualizedResult {
        val ktDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, languageVersionSettings)

        // The ir modules processing is performed phase-to-phase:
        //   1. Collect expect-actual links for classes and their members from dependent fragments
        val (expectActualMap, actualDeclarations) = ExpectActualCollector(
            mainFragment,
            dependentFragments,
            typeSystemContext,
            ktDiagnosticReporter,
            expectActualTracker
        ).collect()

        IrExpectActualCheckers(expectActualMap, actualDeclarations, typeSystemContext, ktDiagnosticReporter).check()

        //   2. Remove top-only expect declarations since they are not needed anymore and should not be presented in the final IrFragment
        //      Expect fake-overrides from non-expect classes remain untouched since they will be actualized in the next phase.
        //      Also, it doesn't remove unactualized expect declarations marked with @OptionalExpectation
        val removedExpectDeclarations = removeExpectDeclarations(dependentFragments, expectActualMap)

        if (!useIrFakeOverrideBuilder) {
            //   3. Actualize expect fake overrides in non-expect classes inside common or multi-platform module.
            //      It's probably important to run FakeOverridesActualizer before ActualFakeOverridesAdder
            FakeOverridesActualizer(expectActualMap).apply { dependentFragments.forEach { visitModuleFragment(it) } }

            //   4. Add fake overrides to non-expect classes inside common or multi-platform module,
            //      taken from these non-expect classes actualized super classes.
            ActualFakeOverridesAdder(
                expectActualMap,
                actualDeclarations.actualClasses,
                ktDiagnosticReporter,
                typeSystemContext
            ).apply { dependentFragments.forEach { visitModuleFragment(it) } }
        }

        //   5. Copy and actualize function parameter default values from expect functions
        val symbolRemapper = ActualizerSymbolRemapper(expectActualMap)
        val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)
        FunctionDefaultParametersActualizer(symbolRemapper, typeRemapper, expectActualMap).actualize()

        //   6. Actualize expect calls in dependent fragments using info obtained in the previous steps
        val actualizerVisitor = ActualizerVisitor(symbolRemapper, typeRemapper)
        dependentFragments.forEach { it.transform(actualizerVisitor, null) }

        //   7. Merge dependent fragments into the main one
        mergeIrFragments(mainFragment, dependentFragments)

        if (useIrFakeOverrideBuilder) {
            //   8. Rebuild fake overrides from stretch, as they could become invalid during actualization
            FakeOverrideRebuilder(symbolTable, fakeOverrideBuilder).rebuildFakeOverrides(mainFragment)
        }

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


/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.processOverriddenFunctionsSafe
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.name.Name

sealed class FirMultipleDefaultsInheritedFromSupertypesChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object Regular : FirMultipleDefaultsInheritedFromSupertypesChecker(MppCheckerKind.Platform) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (declaration.isExpect) return
            super.check(declaration)
        }
    }

    object ForExpectClass : FirMultipleDefaultsInheritedFromSupertypesChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (!declaration.isExpect) return
            super.check(declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration.isExternal) return

        declaration.unsubstitutedScope().processAllFunctions {
            val originalIfSubstitutionOverride = it.unwrapSubstitutionOverrides()

            if (originalIfSubstitutionOverride.containingClassLookupTag() != declaration.symbol.toLookupTag()) {
                return@processAllFunctions
            }

            checkFunction(declaration, originalIfSubstitutionOverride)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFunction(
        declaration: FirClass,
        function: FirNamedFunctionSymbol,
    ) {
        val overriddenFunctions = mutableSetOf<FirNamedFunctionSymbol>()
        function.processOverriddenFunctionsSafe { overridden ->
            // default values of actual functions are located in corresponding expect functions
            val overriddenWithDefaults = overridden.getSingleMatchedExpectForActualOrNull() as? FirNamedFunctionSymbol ?: overridden
            if (overriddenWithDefaults.valueParameterSymbols.any { it.hasDefaultValue }) {
                overriddenFunctions += overriddenWithDefaults
            }
        }
        val isExplicitOverride = function.origin == FirDeclarationOrigin.Source

        val immediateSupertypes = declaration.superConeTypes.mapTo(mutableSetOf()) { it.lookupTag }
        val overriddenFunctionsK1WouldConsider = overriddenFunctions.filter { it.containingClassLookupTag() in immediateSupertypes }

        for ((index, parameter) in function.valueParameterSymbols.withIndex()) {
            val basesWithDefaultValues = overriddenFunctions.filter { it.valueParameterSymbols[index].hasDefaultValue }

            if (basesWithDefaultValues.size <= 1) {
                continue
            }

            val k1WouldMiss = overriddenFunctionsK1WouldConsider.count { it.valueParameterSymbols[index].hasDefaultValue } <= 1

            when {
                !isExplicitOverride -> {
                    reportDiagnosticForImplicitOverride(
                        k1WouldMiss, declaration.source, function.name, parameter, basesWithDefaultValues
                    )
                    // Avoid duplicates
                    break
                }
                else -> reportDiagnosticForExplicitOverride(
                    k1WouldMiss, function.name, parameter, basesWithDefaultValues
                )
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportDiagnosticForImplicitOverride(
        k1WouldMiss: Boolean,
        source: KtSourceElement?,
        name: Name,
        parameter: FirValueParameterSymbol,
        basesWithDefaultValues: List<FirNamedFunctionSymbol>,
    ): Unit = when {
        k1WouldMiss -> reporter.reportOn(
            source,
            FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE_DEPRECATION,
            name,
            parameter,
            basesWithDefaultValues
        )
        else -> reporter.reportOn(
            source,
            FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE,
            name,
            parameter,
            basesWithDefaultValues
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportDiagnosticForExplicitOverride(
        k1WouldMiss: Boolean,
        name: Name,
        parameter: FirValueParameterSymbol,
        basesWithDefaultValues: List<FirNamedFunctionSymbol>,
    ): Unit = when {
        k1WouldMiss -> reporter.reportOn(
            parameter.source,
            FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION,
            name,
            parameter,
            basesWithDefaultValues
        )
        else -> reporter.reportOn(
            parameter.source,
            FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES,
            name,
            parameter,
            basesWithDefaultValues
        )
    }
}

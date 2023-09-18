/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.collectOverriddenFunctionsWhere
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.isSubstitutionOverride
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.name.Name

object FirMultipleDefaultsInheritedFromSupertypesChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.unsubstitutedScope(context).processAllFunctions {
            val originalIfSubstitutionOverride = it.unwrapSubstitutionOverrides()

            if (originalIfSubstitutionOverride.containingClassLookupTag() != declaration.symbol.toLookupTag()) {
                return@processAllFunctions
            }

            checkFunction(declaration, originalIfSubstitutionOverride, context, reporter)
        }
    }

    private fun checkFunction(
        declaration: FirRegularClass,
        function: FirNamedFunctionSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val overriddenFunctions = function.collectOverriddenFunctionsWhere(mutableSetOf(), context) { overridden ->
            // Substitution overrides copy default values from originals
            !overridden.isSubstitutionOverride && overridden.valueParameterSymbols.any { it.hasDefaultValue }
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
                        k1WouldMiss, declaration.source, function.name, parameter, basesWithDefaultValues, context, reporter
                    )
                    // Avoid duplicates
                    break
                }
                else -> reportDiagnosticForExplicitOverride(
                    k1WouldMiss, function.name, parameter, basesWithDefaultValues, context, reporter
                )
            }
        }
    }

    private fun reportDiagnosticForImplicitOverride(
        k1WouldMiss: Boolean,
        source: KtSourceElement?,
        name: Name,
        parameter: FirValueParameterSymbol,
        basesWithDefaultValues: List<FirNamedFunctionSymbol>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ): Unit = when {
        k1WouldMiss -> reporter.reportOn(
            source, FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE_DEPRECATION,
            name, parameter, basesWithDefaultValues, context,
        )
        else -> reporter.reportOn(
            source, FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE,
            name, parameter, basesWithDefaultValues, context,
        )
    }

    private fun reportDiagnosticForExplicitOverride(
        k1WouldMiss: Boolean,
        name: Name,
        parameter: FirValueParameterSymbol,
        basesWithDefaultValues: List<FirNamedFunctionSymbol>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ): Unit = when {
        k1WouldMiss -> reporter.reportOn(
            parameter.source, FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION,
            name, parameter, basesWithDefaultValues, context,
        )
        else -> reporter.reportOn(
            parameter.source, FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES,
            name, parameter, basesWithDefaultValues, context,
        )
    }
}

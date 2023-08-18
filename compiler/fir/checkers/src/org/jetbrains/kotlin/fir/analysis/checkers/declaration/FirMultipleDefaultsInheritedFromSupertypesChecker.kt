/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

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

object FirMultipleDefaultsInheritedFromSupertypesChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.unsubstitutedScope(context).processAllFunctions {
            if (it.containingClassLookupTag() != declaration.symbol.toLookupTag()) {
                return@processAllFunctions
            }

            checkFunction(declaration, it, context, reporter)
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

        for ((index, parameter) in function.valueParameterSymbols.withIndex()) {
            val basesWithDefaultValues = overriddenFunctions.count { it.valueParameterSymbols[index].hasDefaultValue }

            when {
                basesWithDefaultValues <= 1 -> {
                    continue
                }
                !isExplicitOverride -> {
                    reporter.reportOn(
                        declaration.source, FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE,
                        parameter, context,
                    )
                    // Avoid duplicates
                    break
                }
                else -> reporter.reportOn(
                    parameter.source, FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES,
                    parameter, context,
                )
            }
        }
    }
}

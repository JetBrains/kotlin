/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenFunctions
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverrideFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.castAll

object FirMultipleDefaultsInheritedFromSupertypesChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.unsubstitutedScope(context).processAllFunctions {
            checkFunction(declaration, it, context, reporter)
        }
    }

    private fun checkFunction(
        declaration: FirRegularClass,
        function: FirNamedFunctionSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        @OptIn(UnsafeCastFunction::class)
        val overriddenFunctions = when (function) {
            is FirIntersectionOverrideFunctionSymbol -> function.intersections.castAll<FirFunctionSymbol<*>>()
            else -> function.directOverriddenFunctions(context)
        }

        for ((index, parameter) in function.valueParameterSymbols.withIndex()) {
            val basesWithDefaultValues = overriddenFunctions.count { it.valueParameterSymbols[index].hasDefaultValue }

            when {
                basesWithDefaultValues <= 1 -> {
                    continue
                }
                function.isSubstitutionOrIntersectionOverride -> reporter.reportOn(
                    declaration.source, FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE,
                    parameter, context,
                )
                else -> reporter.reportOn(
                    parameter.source, FirErrors.MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES,
                    parameter, context,
                )
            }
        }
    }
}

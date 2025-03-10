/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.explicitReceiverIsNotSuperReference
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.scopes.anyOverriddenOf
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverridesOrDelegated

object FirSuperCallWithDefaultsChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.explicitReceiverIsNotSuperReference()) return

        val functionSymbol = expression.calleeReference.toResolvedNamedFunctionSymbol()
            ?.unwrapFakeOverridesOrDelegated()
            ?.let { it.getSingleMatchedExpectForActualOrNull() ?: it } as? FirNamedFunctionSymbol
            ?: return
        val containingClass = functionSymbol
            .getContainingClassSymbol() as? FirClassSymbol<*>
            ?: return

        fun FirNamedFunctionSymbol.hasDefaultValues(): Boolean =
            !isOverride && valueParameterSymbols.any { it.hasDefaultValue }

        val isCallWithDefaultValues = functionSymbol.hasDefaultValues()
                || containingClass.anyOverriddenOf(functionSymbol, context) { it.hasDefaultValues() }
        val arguments = expression.argumentList as? FirResolvedArgumentList ?: return

        if (isCallWithDefaultValues && arguments.arguments.size < functionSymbol.valueParameterSymbols.size) {
            reporter.reportOn(
                expression.calleeReference.source,
                FirErrors.SUPER_CALL_WITH_DEFAULT_PARAMETERS,
                functionSymbol.name.asString(),
                context
            )
        }
    }

    private fun FirClassSymbol<*>.anyOverriddenOf(
        functionSymbol: FirNamedFunctionSymbol,
        context: CheckerContext,
        predicate: (FirNamedFunctionSymbol) -> Boolean
    ): Boolean {
        val containingScope = unsubstitutedScope(context)
        // Without it, `LLReversedDiagnosticsFe10TestGenerated.testSuperCallsWithDefaultArguments` fails
        // because the maps in the scope are empty.
        containingScope.processFunctionsByName(functionSymbol.name) { }
        return containingScope.anyOverriddenOf(functionSymbol, predicate)
    }
}

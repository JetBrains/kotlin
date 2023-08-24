/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeResolutionResultOverridesOtherToPreserveCompatibility

object FirCustomEnumEntriesMigrationAccessChecker : FirPropertyAccessExpressionChecker() {
    override fun check(expression: FirPropertyAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.PrioritizedEnumEntries)) return
        val referencedSymbol = expression.calleeReference.toResolvedPropertySymbol() ?: return
        if (referencedSymbol.name != StandardNames.ENUM_ENTRIES) return
        if (expression.nonFatalDiagnostics.none { it is ConeResolutionResultOverridesOtherToPreserveCompatibility }) return

        // This 'if' is needed just to choose one of two diagnostics
        if (expression.dispatchReceiver is FirResolvedQualifier || expression.extensionReceiver is FirResolvedQualifier) {
            reporter.reportOn(expression.source, FirErrors.DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY, context)
        } else {
            reporter.reportOn(expression.source, FirErrors.DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM, context)
        }
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeResolutionResultOverridesOtherToPreserveCompatibility
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

object FirCustomEnumEntriesMigrationAccessChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        if (LanguageFeature.PrioritizedEnumEntries.isEnabled()) return
        val referencedSymbol = expression.calleeReference.toResolvedPropertySymbol() ?: return
        if (referencedSymbol.name != StandardNames.ENUM_ENTRIES) return
        if (expression.nonFatalDiagnostics.none { it is ConeResolutionResultOverridesOtherToPreserveCompatibility }) return

        // This 'if' is needed just to choose one of two diagnostics
        if (expression.dispatchReceiver?.unwrapSmartcastExpression() is FirResolvedQualifier ||
            expression.extensionReceiver?.unwrapSmartcastExpression() is FirResolvedQualifier
        ) {
            reporter.reportOn(expression.source, FirErrors.DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY)
        } else if (context.containingDeclarations.any { it is FirClassSymbol<*> && it.isEnumClass }) {
            reporter.reportOn(expression.source, FirErrors.DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM)
        } else {
            reporter.reportOn(expression.source, FirErrors.DEPRECATED_ACCESS_TO_ENTRIES_PROPERTY)
        }
    }
}

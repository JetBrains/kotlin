/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDeprecated

object FirDeprecatedQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        expression.nonFatalDiagnostics.filterIsInstance<ConeDeprecated>().forEach { diagnostic ->
            FirDeprecationChecker.reportApiStatus(
                diagnostic.source, diagnostic.symbol, isTypealiasExpansion = false,
                diagnostic.deprecationInfo
            )
        }
        if (expression.resolvedToCompanionObject) {
            // Accessing the companion is like following a chain:
            // TA1 -> TA2 -> ... -> MyClass ~> Companion.
            // The first part - `TA1 -> TA2 -> ... -> MyClass` -
            // is handled automatically when getting deprecationInfo
            // for the typealias symbol (in FirDeprecationChecker).
            // Below we check "the last transition".
            val companionSymbol = expression.symbol?.fullyExpandedClass()?.resolvedCompanionObjectSymbol ?: return
            FirDeprecationChecker.reportApiStatusIfNeeded(expression.source, companionSymbol)
        }
    }
}

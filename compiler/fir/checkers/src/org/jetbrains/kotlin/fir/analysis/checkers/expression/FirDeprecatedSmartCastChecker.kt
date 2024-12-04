/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.isDelegated
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.resolvedType

object FirDeprecatedSmartCastChecker : FirSmartCastExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirSmartCastExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.UnstableSmartcastOnDelegatedProperties)) return // No need to run this checker
        if (!expression.isStable) return // Unstable smartcasts are already errors.

        val source = expression.source ?: return
        val symbol = expression.originalExpression.toReference(context.session)?.toResolvedSymbol<FirPropertySymbol>() ?: return
        if (symbol.isDelegated) {
            reporter.reportOn(source, FirErrors.DEPRECATED_SMARTCAST_ON_DELEGATED_PROPERTY, expression.resolvedType, symbol, context)
        }
    }
}

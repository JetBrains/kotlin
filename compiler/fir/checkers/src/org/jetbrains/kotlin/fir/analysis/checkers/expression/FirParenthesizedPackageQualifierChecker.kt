/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.secondToLastContainer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES
import org.jetbrains.kotlin.resolve.source.hasUnwrappableAsExplicitReceiver

object FirParenthesizedPackageQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        if (LanguageFeature.ForbidAnnotationsTypeArgumentsAndParenthesesForPackageQualifier.isDisabled()) return
        val source = expression.source ?: return
        if (expression.symbol != null || source.kind is KtFakeSourceElementKind) return

        if (checkOutermostPackage(expression)) return
        checkNestedPackages(source)
    }

    /**
     * In `part1.part2.part3.foo()` checks `part1.part2.part3`.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOutermostPackage(expression: FirResolvedQualifier): Boolean {
        val containingElement = context.secondToLastContainer ?: return false
        if (expression.isExplicitReceiverOf(containingElement) && containingElement.source.hasUnwrappableAsExplicitReceiver()) {
            reporter.reportOn(
                containingElement.source,
                FirErrors.PARENTHESIZED_PACKAGE_QUALIFIER,
                positioningStrategy = SourceElementPositioningStrategies.RECEIVER_OF_DOT_QUALIFIED,
            )
            return true
        }
        return false
    }

    private fun FirResolvedQualifier.isExplicitReceiverOf(other: FirElement): Boolean {
        return when (other) {
            is FirResolvedQualifier -> this == other.explicitParent
            is FirQualifiedAccessExpression -> this == other.explicitReceiver
            else -> false
        }
    }

    /**
     * In `part1.part2.part3.foo()` checks `part1.part2` and `part1`.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkNestedPackages(source: KtSourceElement) {
        val sourceOfParenthesized = source.getChild(UNWRAPPABLE_TOKEN_TYPES)
        if (sourceOfParenthesized != null) {
            reporter.reportOn(sourceOfParenthesized, FirErrors.PARENTHESIZED_PACKAGE_QUALIFIER)
        }
    }
}

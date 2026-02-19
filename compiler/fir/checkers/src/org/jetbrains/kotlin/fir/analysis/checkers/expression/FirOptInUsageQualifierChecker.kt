/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.isExperimentalMarker
import org.jetbrains.kotlin.fir.analysis.checkers.resolvedCompanionSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

object FirOptInUsageQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        checkNotAcceptedExperimentalities(expression)
        checkMarkerUsedAsQualifier(expression)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkNotAcceptedExperimentalities(
        expression: FirResolvedQualifier,
    ) {
        val symbol = expression.symbol ?: return
        val companionObjectSymbol = expression.resolvedCompanionSymbol()
        with(FirOptInUsageBaseChecker) {
            val (hardExperimentalities, softExperimentalities) = symbol.loadExperimentalitiesForQualifier(companionObjectSymbol)
            reportNotAcceptedExperimentalities(hardExperimentalities, expression)
            reportNotAcceptedExperimentalities(
                softExperimentalities,
                expression,
                reportErrorsAsDeprecationWarnings = LanguageFeature.ReportOptInUsageOnCompanionObjectAccesses.isDisabled(),
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMarkerUsedAsQualifier(
        expression: FirResolvedQualifier,
    ) {
        val containingElements = context.containingElements
        val parentExpression = containingElements.lastOrNull { it is FirQualifiedAccessExpression && it.dispatchReceiver == expression }
        val source = parentExpression?.source ?: return
        expression.symbol?.checkContainingClasses(source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private tailrec fun FirClassLikeSymbol<*>.checkContainingClasses(
        source: KtSourceElement,
    ) {
        if (isExperimentalMarker(context.session) && context.containingDeclarations.none { it == this }) {
            reporter.reportOn(source, FirErrors.OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN)
        }

        val containingClassSymbol = this.getContainingClassLookupTag()?.toSymbol() ?: return
        containingClassSymbol.checkContainingClasses(source)
    }
}

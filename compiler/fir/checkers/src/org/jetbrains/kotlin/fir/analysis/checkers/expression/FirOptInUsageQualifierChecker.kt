/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.isExperimentalMarker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

object FirOptInUsageQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        checkNotAcceptedExperimentalities(expression, context, reporter)
        checkMarkerUsedAsQualifier(expression, context, reporter)
    }

    private fun checkNotAcceptedExperimentalities(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.symbol ?: return
        with(FirOptInUsageBaseChecker) {
            val experimentalities = symbol.loadExperimentalities(context, fromSetter = false, dispatchReceiverType = null)
            reportNotAcceptedExperimentalities(experimentalities, expression, context, reporter)
        }
    }

    private fun checkMarkerUsedAsQualifier(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingElements = context.containingElements
        val parentExpression = containingElements.lastOrNull { it is FirQualifiedAccessExpression && it.dispatchReceiver == expression }
        val source = parentExpression?.source ?: return
        expression.symbol?.checkContainingClasses(source, context, reporter)
    }

    private tailrec fun FirClassLikeSymbol<*>.checkContainingClasses(
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (isExperimentalMarker(context.session) && context.containingDeclarations.none { it.symbol == this }) {
            reporter.reportOn(source, FirErrors.OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN, context)
        }

        val containingClassSymbol = this.getContainingClassLookupTag()?.toSymbol(context.session) ?: return
        containingClassSymbol.checkContainingClasses(source, context, reporter)
    }
}

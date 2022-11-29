/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.getDeprecation
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolved
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

object FirDeprecationChecker : FirBasicExpressionChecker() {

    private val allowedSourceKinds = setOf(
        KtRealSourceElementKind,
        KtFakeSourceElementKind.DesugaredIncrementOrDecrement
    )

    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!allowedSourceKinds.contains(expression.source?.kind)) return
        if (expression is FirAnnotation || expression is FirDelegatedConstructorCall) return //checked by FirDeprecatedTypeChecker
        val resolvable = expression as? FirResolvable ?: return
        val reference = resolvable.calleeReference.resolved ?: return
        val referencedSymbol = reference.resolvedSymbol

        reportDeprecationIfNeeded(reference.source, referencedSymbol, expression, context, reporter)
    }

    internal fun reportDeprecationIfNeeded(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        callSite: FirElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val deprecation = getWorstDeprecation(callSite, referencedSymbol, context) ?: return
        reportDeprecation(source, referencedSymbol, deprecation, reporter, context)
    }

    internal fun reportDeprecation(
        source: KtSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        deprecationInfo: DeprecationInfo,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        val diagnostic = when (deprecationInfo.deprecationLevel) {
            DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> FirErrors.DEPRECATION_ERROR
            DeprecationLevelValue.WARNING -> FirErrors.DEPRECATION
        }
        reporter.reportOn(source, diagnostic, referencedSymbol, deprecationInfo.message ?: "", context)
    }

    private fun getWorstDeprecation(
        callSite: FirElement?,
        symbol: FirBasedSymbol<*>,
        context: CheckerContext
    ): DeprecationInfo? {
        val deprecationInfos = listOfNotNull(
            symbol.getDeprecation(context.session, callSite),
            (symbol as? FirConstructorSymbol)
                ?.resolvedReturnTypeRef
                ?.toRegularClassSymbol(context.session)
                ?.getDeprecation(context.session, callSite)
        )
        return deprecationInfos.maxOrNull()
    }
}

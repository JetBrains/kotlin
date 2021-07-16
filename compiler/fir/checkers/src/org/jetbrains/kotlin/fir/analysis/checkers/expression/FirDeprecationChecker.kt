/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.Deprecation
import org.jetbrains.kotlin.fir.declarations.DeprecationLevelValue
import org.jetbrains.kotlin.fir.declarations.getDeprecation
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirDeprecationChecker : FirBasicExpressionChecker() {

    private val allowedSourceKinds = setOf(
        FirRealSourceElementKind,
        FirFakeSourceElementKind.DesugaredIncrementOrDecrement
    )

    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!allowedSourceKinds.contains(expression.source?.kind)) return
        if (expression is FirAnnotationCall) return //checked by FirDeprecatedTypeChecker
        val resolvable = expression as? FirResolvable ?: return
        val reference = resolvable.calleeReference as? FirResolvedNamedReference ?: return
        val referencedSymbol = reference.resolvedSymbol

        reportDeprecationIfNeeded(expression.source, referencedSymbol, expression, context, reporter)
    }

    internal fun reportDeprecationIfNeeded(
        source: FirSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        callSite: FirElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val deprecation = getWorstDeprecation(callSite, referencedSymbol, context) ?: return
        reportDeprecation(source, referencedSymbol, deprecation, reporter, context)
    }

    internal fun reportDeprecation(
        source: FirSourceElement?,
        referencedSymbol: FirBasedSymbol<*>,
        deprecation: Deprecation,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        val diagnostic = when (deprecation.level) {
            DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> FirErrors.DEPRECATION_ERROR
            DeprecationLevelValue.WARNING -> FirErrors.DEPRECATION
        }
        reporter.reportOn(source, diagnostic, referencedSymbol, deprecation.message ?: "", context)
    }

    private fun getWorstDeprecation(
        callSite: FirElement?,
        symbol: FirBasedSymbol<*>,
        context: CheckerContext
    ): Deprecation? {
        val deprecationInfos = listOfNotNull(
            symbol.getDeprecation(callSite),
            symbol.safeAs<FirConstructorSymbol>()
                ?.resolvedReturnTypeRef
                ?.toRegularClassSymbol(context.session)
                ?.getDeprecation(callSite)
        )
        return deprecationInfos.maxOrNull()
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
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
        val referencedFir = reference.resolvedSymbol.fir
        if (referencedFir !is FirAnnotatedDeclaration) return

        reportDeprecationIfNeeded(expression.source, referencedFir, expression, context, reporter)
    }

    internal fun <T> reportDeprecationIfNeeded(
        source: FirSourceElement?,
        referencedFir: T,
        callSite: FirElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) where T : FirAnnotatedDeclaration {
        val deprecation = getWorstDeprecation(callSite, referencedFir, context) ?: return
        val diagnostic = when (deprecation.level) {
            DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> FirErrors.DEPRECATION_ERROR
            DeprecationLevelValue.WARNING -> FirErrors.DEPRECATION
        }
        reporter.reportOn(source, diagnostic, referencedFir.symbol, deprecation.message ?: "", context)
    }

    private fun <T : FirAnnotatedDeclaration> getWorstDeprecation(
        callSite: FirElement?,
        fir: T,
        context: CheckerContext
    ): Deprecation? {
        val deprecationInfos = listOfNotNull(
            fir.getDeprecation(callSite),
            fir.safeAs<FirConstructor>()?.returnTypeRef
                ?.toRegularClass(context.session)
                ?.getDeprecation(callSite)
        )
        return deprecationInfos.maxOrNull()
    }

}
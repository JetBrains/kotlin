/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedReifiedParameterReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeParameterInQualifiedAccess
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

object FirTypeParameterInQualifiedAccessChecker : FirQualifiedAccessChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        checkExplicitReceiver(expression, context, reporter)
        checkExpressionItself(expression, context, reporter)
    }

    private fun checkExpressionItself(
        expression: FirQualifiedAccessExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // Make sure the current qualified access is not part of another qualified access or class literals.
        // E.g., for `T::toString`, which is a callable reference (a subtype of qualified access), type parameter T is checked once as an
        // explicit receiver (or LHS). When we visit `T` (as a qualified access), we should not regard it as an expression here.
        if (context.qualifiedAccesses.size > 1 || context.getClassCalls.isNotEmpty()) return

        val diagnostic = expression.typeRef.coneTypeParameterInQualifiedAccess ?: return
        val source = expression.source ?: return
        reporter.reportOn(source, FirErrors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION, diagnostic.symbol, context)
    }

    private fun checkExplicitReceiver(
        expression: FirQualifiedAccessExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val explicitReceiver = expression.explicitReceiver
        val typeParameterSymbol =
            (explicitReceiver as? FirResolvedReifiedParameterReference)?.symbol
                ?: explicitReceiver?.typeRef?.coneTypeParameterInQualifiedAccess?.symbol
                ?: return
        if (expression is FirCallableReferenceAccess) {
            reporter.reportOn(expression.source, FirErrors.CALLABLE_REFERENCE_LHS_NOT_A_CLASS, context)
        } else {
            reporter.reportOn(explicitReceiver?.source, FirErrors.TYPE_PARAMETER_ON_LHS_OF_DOT, typeParameterSymbol, context)
        }
    }

    private val FirTypeRef.coneTypeParameterInQualifiedAccess: ConeTypeParameterInQualifiedAccess?
        get() = (this as? FirErrorTypeRef)?.diagnostic as? ConeTypeParameterInQualifiedAccess
}

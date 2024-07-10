/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedReifiedParameterReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeParameterInQualifiedAccess
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType

object FirTypeParameterInQualifiedAccessChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        checkExplicitReceiver(expression, context, reporter)
        checkExpressionItself(expression, context, reporter)
    }

    private fun checkExpressionItself(
        expression: FirQualifiedAccessExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // Ignore T::class where our expression is T
        if (context.getClassCalls.lastOrNull()?.argument == expression) return

        // Make sure the current expression is not the receiver of a qualified access expression.
        // E.g., for `T::toString`, which is a callable reference (a subtype of qualified access), type parameter T is checked once as an
        // explicit receiver. When we visit `T` (as a qualified access expression), we should not regard it as an expression here.
        val secondLast = context.callsOrAssignments.elementAtOrNull(context.callsOrAssignments.size - 2)
        if (secondLast is FirQualifiedAccessExpression && secondLast.explicitReceiver == expression) return

        val diagnostic = expression.resolvedType.coneTypeParameterInQualifiedAccess ?: return
        val source = expression.calleeReference.source ?: return
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
                ?: explicitReceiver?.resolvedType?.coneTypeParameterInQualifiedAccess?.symbol
                ?: return
        if (expression is FirCallableReferenceAccess) {
            reporter.reportOn(expression.source, FirErrors.CALLABLE_REFERENCE_LHS_NOT_A_CLASS, context)
        } else {
            reporter.reportOn(explicitReceiver?.source, FirErrors.TYPE_PARAMETER_ON_LHS_OF_DOT, typeParameterSymbol, context)
        }
    }

    private val ConeKotlinType.coneTypeParameterInQualifiedAccess: ConeTypeParameterInQualifiedAccess?
        get() = (this as? ConeErrorType)?.diagnostic as? ConeTypeParameterInQualifiedAccess
}

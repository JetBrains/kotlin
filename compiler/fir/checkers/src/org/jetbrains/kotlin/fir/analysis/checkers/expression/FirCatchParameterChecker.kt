/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeOfThrowable
import org.jetbrains.kotlin.fir.analysis.checkers.throwableClassLikeType
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType

object FirCatchParameterChecker : FirTryExpressionChecker() {
    override fun check(expression: FirTryExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        for (catchEntry in expression.catches) {
            val catchParameter = catchEntry.parameter

            if (catchParameter.defaultValue != null) {
                reporter.reportOn(catchParameter.source, FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE, context)
            }

            val coneType = catchParameter.returnTypeRef.coneType
            if (coneType is ConeTypeParameterType) {
                val isReified = coneType.lookupTag.typeParameterSymbol.fir.isReified

                if (isReified) {
                    reporter.reportOn(catchParameter.source, FirErrors.REIFIED_TYPE_IN_CATCH_CLAUSE, context)
                } else {
                    reporter.reportOn(catchParameter.source, FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE, context)
                }
            }

            val session = context.session
            if (!coneType.isSubtypeOfThrowable(session)) {
                reporter.reportOn(catchParameter.source, FirErrors.TYPE_MISMATCH, throwableClassLikeType(session), coneType, context)
            }
        }
    }
}

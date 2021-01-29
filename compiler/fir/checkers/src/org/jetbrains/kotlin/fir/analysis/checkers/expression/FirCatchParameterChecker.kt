/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

object FirCatchParameterChecker : FirTryExpressionChecker() {
    override fun check(expression: FirTryExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        for (catchEntry in expression.catches) {
            val catchParameter = catchEntry.parameter

            if (catchParameter.defaultValue != null)
                reporter.report(catchParameter.source?.let { FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE.on(it) })

            val typeRef = catchParameter.returnTypeRef
            if (typeRef is FirResolvedTypeRef && typeRef.type is ConeTypeParameterType) {
                val isReified = (typeRef.type as ConeTypeParameterType).lookupTag.typeParameterSymbol.fir.isReified

                if (isReified) {
                    reporter.report(catchParameter.source?.let { FirErrors.REIFIED_TYPE_IN_CATCH_CLAUSE.on(it) })
                } else {
                    reporter.report(catchParameter.source?.let { FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE.on(it) })
                }
            }
        }
    }
}
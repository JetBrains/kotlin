/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.FirReturnsImpliesAnalyzer.isSupertypeOf
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isThrowable
import org.jetbrains.kotlin.fir.analysis.checkers.throwableClassLikeType
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.typeCheckerContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType

object FirCatchParameterChecker : FirTryExpressionChecker() {
    override fun check(expression: FirTryExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        for (catchEntry in expression.catches) {
            val catchParameter = catchEntry.parameter

            if (catchParameter.defaultValue != null)
                catchParameter.source?.let { reporter.report(FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE.on(it)) }

            val typeRef = catchParameter.returnTypeRef
            if (typeRef !is FirResolvedTypeRef) return

            val coneType = typeRef.type
            if (coneType is ConeTypeParameterType) {
                val isReified = coneType.lookupTag.typeParameterSymbol.fir.isReified

                if (isReified) {
                    catchParameter.source?.let { reporter.report(FirErrors.REIFIED_TYPE_IN_CATCH_CLAUSE.on(it)) }
                } else {
                    catchParameter.source?.let { reporter.report(FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE.on(it)) }
                }
            }

            if (!coneType.isThrowable(context.session))
                catchParameter.source?.let { reporter.report(FirErrors.TYPE_MISMATCH.on(it, throwableClassLikeType, coneType)) }
        }
    }
}
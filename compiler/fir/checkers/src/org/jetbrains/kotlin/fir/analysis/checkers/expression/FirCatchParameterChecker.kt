/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeOfThrowable
import org.jetbrains.kotlin.fir.analysis.checkers.valOrVarKeyword
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.isTypeMismatchDueToNullability
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType

object FirCatchParameterChecker : FirTryExpressionChecker() {
    override fun check(expression: FirTryExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        for (catchEntry in expression.catches) {
            val catchParameter = catchEntry.parameter
            val source = catchParameter.source ?: continue

            if (catchParameter.defaultValue != null) {
                reporter.reportOn(source, FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE, context)
            }

            source.valOrVarKeyword?.let {
                reporter.reportOn(source, FirErrors.VAL_OR_VAR_ON_CATCH_PARAMETER, it, context)
            }

            val coneType = catchParameter.returnTypeRef.coneType
            if (coneType is ConeTypeParameterType) {
                val isReified = coneType.lookupTag.typeParameterSymbol.isReified

                if (isReified) {
                    reporter.reportOn(source, FirErrors.REIFIED_TYPE_IN_CATCH_CLAUSE, context)
                } else {
                    reporter.reportOn(source, FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE, context)
                }
            }

            val session = context.session
            if (!coneType.isSubtypeOfThrowable(session)) {
                reporter.reportOn(
                    source,
                    FirErrors.THROWABLE_TYPE_MISMATCH,
                    coneType,
                    context.session.inferenceComponents.ctx.isTypeMismatchDueToNullability(
                        coneType,
                        session.builtinTypes.throwableType.type
                    ),
                    context
                )
            }
        }
    }
}

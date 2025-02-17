/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.defaultValueForParameter
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeOfThrowable
import org.jetbrains.kotlin.fir.analysis.checkers.valOrVarKeyword
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.involvesCapturedTypes
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.isTypeMismatchDueToNullability
import org.jetbrains.kotlin.fir.types.typeContext

object FirCatchParameterChecker : FirTryExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirTryExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        for (catchEntry in expression.catches) {
            val catchParameter = catchEntry.parameter
            val source = catchParameter.source ?: continue

            if (catchParameter.source?.defaultValueForParameter != null) {
                reporter.reportOn(source, FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE, context)
            }

            source.valOrVarKeyword?.let {
                reporter.reportOn(source, FirErrors.VAL_OR_VAR_ON_CATCH_PARAMETER, it, context)
            }

            val coneType = catchParameter.returnTypeRef.coneType
            if (coneType is ConeTypeParameterType || coneType is ConeDefinitelyNotNullType) {
                val isReified = with(context.session.typeContext) {
                    (coneType.originalIfDefinitelyNotNullable() as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol?.isReified == true
                }

                if (isReified) {
                    reporter.reportOn(source, FirErrors.REIFIED_TYPE_IN_CATCH_CLAUSE, context)
                } else {
                    reporter.reportOn(source, FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE, context)
                }
            }

            val session = context.session
            if (!coneType.isSubtypeOfThrowable(session) || isProhibitedNothing(context, coneType)) {
                reporter.reportOn(
                    source,
                    FirErrors.THROWABLE_TYPE_MISMATCH,
                    coneType,
                    context.session.typeContext.isTypeMismatchDueToNullability(
                        coneType,
                        session.builtinTypes.throwableType.coneType
                    ),
                    context.session.typeContext.involvesCapturedTypes(coneType),
                    context
                )
            }
        }
    }

    private fun isProhibitedNothing(context: CheckerContext, coneType: ConeKotlinType): Boolean {
        return context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitNothingAsCatchParameter) && coneType.isNothing
    }
}

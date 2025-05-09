/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.isTypeMismatchDueToNullability
import org.jetbrains.kotlin.fir.types.typeContext

object FirCatchParameterChecker : FirTryExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirTryExpression) {
        for (catchEntry in expression.catches) {
            val catchParameter = catchEntry.parameter
            val source = catchParameter.source ?: continue

            if (catchParameter.source?.defaultValueForParameter != null) {
                reporter.reportOn(source, FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE)
            }

            source.valOrVarKeyword?.let {
                reporter.reportOn(source, FirErrors.VAL_OR_VAR_ON_CATCH_PARAMETER, it)
            }

            val coneType = catchParameter.returnTypeRef.coneType
            if (coneType is ConeTypeParameterType || coneType is ConeDefinitelyNotNullType) {
                val isReified = with(context.session.typeContext) {
                    (coneType.originalIfDefinitelyNotNullable() as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol?.isReified == true
                }

                if (isReified) {
                    catchParameter.requireFeatureSupport(LanguageFeature.AllowReifiedTypeInCatchClause, context, reporter)
                } else {
                    reporter.reportOn(source, FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE)
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
                    )
                )
            }
        }
    }

    private fun isProhibitedNothing(context: CheckerContext, coneType: ConeKotlinType): Boolean {
        return context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitNothingAsCatchParameter) && coneType.isNothing
    }
}

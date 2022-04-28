/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.CastingType
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isCastErased
import org.jetbrains.kotlin.fir.analysis.checkers.checkCasting
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.coneType

object FirCastOperatorsChecker : FirTypeOperatorCallChecker() {
    override fun check(expression: FirTypeOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val firstArgument = expression.argumentList.arguments[0]
        val actualType = (if (firstArgument is FirExpressionWithSmartcast) {
            firstArgument.originalType.coneType
        } else {
            firstArgument.typeRef.coneType
        }).fullyExpandedType(session)
        val conversionTypeRef = expression.conversionTypeRef
        val targetType = conversionTypeRef.coneType.fullyExpandedType(session)

        val isSafeAs = expression.operation == FirOperation.SAFE_AS
        if (expression.operation == FirOperation.AS || isSafeAs) {
            val castType = checkCasting(actualType, targetType, isSafeAs, context)
            if (castType == CastingType.Impossible) {
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2)) {
                    reporter.reportOn(expression.source, FirErrors.CAST_NEVER_SUCCEEDS, context)
                }
            } else if (castType == CastingType.Always) {
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2)) {
                    reporter.reportOn(expression.source, FirErrors.USELESS_CAST, context)
                }
            } else if (isCastErased(actualType, targetType, context)) {
                reporter.reportOn(expression.source, FirErrors.UNCHECKED_CAST, actualType, targetType, context)
            }
        } else if (expression.operation == FirOperation.IS) {
            if (isCastErased(actualType, targetType, context)) {
                reporter.reportOn(conversionTypeRef.source, FirErrors.CANNOT_CHECK_FOR_ERASED, targetType, context)
            }
        }
    }
}
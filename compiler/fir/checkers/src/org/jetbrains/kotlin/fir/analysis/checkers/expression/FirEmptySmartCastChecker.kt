/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.transformers.FirWhenExhaustivenessTransformer
import org.jetbrains.kotlin.fir.types.isNothingOrNullableNothing
import org.jetbrains.kotlin.fir.types.isResolved
import org.jetbrains.kotlin.fir.types.resolvedType

object FirEmptySmartCastChecker : FirSmartCastExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirSmartCastExpression) {
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.DataFlowBasedExhaustiveness)) return
        if (!expression.isStable || !expression.isResolved || expression.resolvedType.isNothingOrNullableNothing) return
        if (expression.originalExpression is FirWhenSubjectExpression) return
        if (expression.lowerTypesFromSmartCast.isEmpty()) return
        if (FirWhenExhaustivenessTransformer.coversAllCases(context.session, expression.resolvedType, expression.lowerTypesFromSmartCast)) {
            reporter.reportOn(expression.source, FirErrors.POTENTIALLY_NOTHING_VALUE)
        }
    }

}
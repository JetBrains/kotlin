/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.types.ConstantValueKind

abstract class FirMultiDollarInterpolationChecker<E : FirExpression> : FirExpressionChecker<E>(MppCheckerKind.Common) {
    abstract fun E.getInterpolationPrefix(): String?

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: E) {
        // no interpolation prefix => always OK
        if (expression.getInterpolationPrefix().isNullOrEmpty()) return

        expression.requireFeatureSupport(LanguageFeature.MultiDollarInterpolation)
    }
}

object FirMultiDollarInterpolationCheckerConcatenation : FirMultiDollarInterpolationChecker<FirStringConcatenationCall>() {
    override fun FirStringConcatenationCall.getInterpolationPrefix(): String = interpolationPrefix
}

object FirMultiDollarInterpolationCheckerLiteral : FirMultiDollarInterpolationChecker<FirLiteralExpression>() {
    override fun FirLiteralExpression.getInterpolationPrefix(): String? =
        prefix?.takeIf { this.kind == ConstantValueKind.String }
}

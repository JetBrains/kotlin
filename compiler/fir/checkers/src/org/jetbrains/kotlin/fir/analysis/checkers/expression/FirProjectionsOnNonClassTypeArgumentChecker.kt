/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.types.FirStarProjection
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.types.Variance

object FirProjectionsOnNonClassTypeArgumentChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        for (it in expression.typeArguments) {
            when (it) {
                is FirStarProjection -> reporter.reportOn(it.source, FirErrors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT, context)
                is FirTypeProjectionWithVariance -> {
                    if (it.variance != Variance.INVARIANT) {
                        val modifierSource = it.source.getModifierList()?.modifiers?.firstOrNull()?.source
                        reporter.reportOn(modifierSource ?: it.source, FirErrors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT, context)
                    }
                }
            }
        }
    }
}

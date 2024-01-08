/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.hasDiagnosticKind
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLambdaArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type

object FirRecursiveProblemChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            expression !is FirExpression ||
            expression.source?.kind is KtFakeSourceElementKind
        ) {
            return
        }

        fun checkConeType(coneType: ConeKotlinType?) {
            if (coneType?.hasDiagnosticKind(DiagnosticKind.RecursionInImplicitTypes) == true) {
                val source = ((expression as? FirLambdaArgumentExpression)?.expression ?: expression).source
                reporter.reportOn(source, FirErrors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM, context)
            } else if (coneType is ConeClassLikeType) {
                for (typeArgument in coneType.typeArguments) {
                    checkConeType(typeArgument.type)
                }
            }
        }

        checkConeType(expression.resolvedType)
    }
}

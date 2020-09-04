/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment

abstract class FirExpressionChecker<in E : FirStatement> {
    abstract fun check(expression: E, context: CheckerContext, reporter: DiagnosticReporter)
}

typealias FirBasicExpressionChecker = FirExpressionChecker<FirStatement>
typealias FirQualifiedAccessChecker = FirExpressionChecker<FirQualifiedAccessExpression>
typealias FirFunctionCallChecker = FirExpressionChecker<FirFunctionCall>
typealias FirVariableAssignmentChecker = FirExpressionChecker<FirVariableAssignment>

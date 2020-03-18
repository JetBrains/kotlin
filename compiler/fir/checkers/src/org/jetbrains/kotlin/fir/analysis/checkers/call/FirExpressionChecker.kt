/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.call

import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression

abstract class FirExpressionChecker<in E : FirExpression> {
    abstract fun check(functionCall: E, reporter: DiagnosticReporter)
}

typealias FirFunctionCallChecker = FirExpressionChecker<FirFunctionCall>
typealias FirQualifiedAccessChecker = FirExpressionChecker<FirQualifiedAccessExpression>
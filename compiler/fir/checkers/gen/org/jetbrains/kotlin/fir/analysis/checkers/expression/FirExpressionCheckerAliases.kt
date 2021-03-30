/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression

typealias FirBasicExpressionChecker = FirExpressionChecker<FirStatement>
typealias FirQualifiedAccessChecker = FirExpressionChecker<FirQualifiedAccessExpression>
typealias FirFunctionCallChecker = FirExpressionChecker<FirFunctionCall>
typealias FirVariableAssignmentChecker = FirExpressionChecker<FirVariableAssignment>
typealias FirTryExpressionChecker = FirExpressionChecker<FirTryExpression>
typealias FirWhenExpressionChecker = FirExpressionChecker<FirWhenExpression>

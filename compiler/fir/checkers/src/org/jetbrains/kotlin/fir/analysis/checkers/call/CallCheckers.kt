/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.call

import org.jetbrains.kotlin.fir.expressions.FirExpression

object CallCheckers {
    val EXPRESSIONS: List<FirExpressionChecker<FirExpression>> = listOf()
    val QUALIFIED_ACCESS: List<FirQualifiedAccessChecker> = listOf<FirQualifiedAccessChecker>() + EXPRESSIONS
    val FUNCTION_CALLS: List<FirFunctionCallChecker> = listOf<FirFunctionCallChecker>() + QUALIFIED_ACCESS
}
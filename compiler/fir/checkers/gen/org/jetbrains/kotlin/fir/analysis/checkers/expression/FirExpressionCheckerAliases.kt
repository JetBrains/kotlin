/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression

typealias FirBasicExpressionChecker = FirExpressionChecker<FirStatement>
typealias FirQualifiedAccessChecker = FirExpressionChecker<FirQualifiedAccessExpression>
typealias FirFunctionCallChecker = FirExpressionChecker<FirFunctionCall>
typealias FirVariableAssignmentChecker = FirExpressionChecker<FirVariableAssignment>
typealias FirTryExpressionChecker = FirExpressionChecker<FirTryExpression>
typealias FirWhenExpressionChecker = FirExpressionChecker<FirWhenExpression>
typealias FirReturnExpressionChecker = FirExpressionChecker<FirReturnExpression>
typealias FirBlockChecker = FirExpressionChecker<FirBlock>
typealias FirAnnotationCallChecker = FirExpressionChecker<FirAnnotationCall>
typealias FirCheckNotNullCallChecker = FirExpressionChecker<FirCheckNotNullCall>
typealias FirElvisExpressionChecker = FirExpressionChecker<FirElvisExpression>
typealias FirGetClassCallChecker = FirExpressionChecker<FirGetClassCall>
typealias FirSafeCallExpressionChecker = FirExpressionChecker<FirSafeCallExpression>
typealias FirEqualityOperatorCallChecker = FirExpressionChecker<FirEqualityOperatorCall>
typealias FirAnonymousFunctionAsExpressionChecker = FirExpressionChecker<FirAnonymousFunction>
typealias FirStringConcatenationCallChecker = FirExpressionChecker<FirStringConcatenationCall>

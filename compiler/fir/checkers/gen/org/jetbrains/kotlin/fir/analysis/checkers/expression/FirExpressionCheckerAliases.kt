/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirBinaryLogicExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirInaccessibleReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirIntegerLiteralOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirLoopJump
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop

typealias FirBasicExpressionChecker = FirExpressionChecker<FirStatement>
typealias FirQualifiedAccessExpressionChecker = FirExpressionChecker<FirQualifiedAccessExpression>
typealias FirCallChecker = FirExpressionChecker<FirCall>
typealias FirFunctionCallChecker = FirExpressionChecker<FirFunctionCall>
typealias FirPropertyAccessExpressionChecker = FirExpressionChecker<FirPropertyAccessExpression>
typealias FirIntegerLiteralOperatorCallChecker = FirExpressionChecker<FirIntegerLiteralOperatorCall>
typealias FirVariableAssignmentChecker = FirExpressionChecker<FirVariableAssignment>
typealias FirTryExpressionChecker = FirExpressionChecker<FirTryExpression>
typealias FirWhenExpressionChecker = FirExpressionChecker<FirWhenExpression>
typealias FirLoopExpressionChecker = FirExpressionChecker<FirLoop>
typealias FirLoopJumpChecker = FirExpressionChecker<FirLoopJump>
typealias FirLogicExpressionChecker = FirExpressionChecker<FirBinaryLogicExpression>
typealias FirReturnExpressionChecker = FirExpressionChecker<FirReturnExpression>
typealias FirBlockChecker = FirExpressionChecker<FirBlock>
typealias FirAnnotationChecker = FirExpressionChecker<FirAnnotation>
typealias FirAnnotationCallChecker = FirExpressionChecker<FirAnnotationCall>
typealias FirCheckNotNullCallChecker = FirExpressionChecker<FirCheckNotNullCall>
typealias FirElvisExpressionChecker = FirExpressionChecker<FirElvisExpression>
typealias FirGetClassCallChecker = FirExpressionChecker<FirGetClassCall>
typealias FirSafeCallExpressionChecker = FirExpressionChecker<FirSafeCallExpression>
typealias FirEqualityOperatorCallChecker = FirExpressionChecker<FirEqualityOperatorCall>
typealias FirStringConcatenationCallChecker = FirExpressionChecker<FirStringConcatenationCall>
typealias FirTypeOperatorCallChecker = FirExpressionChecker<FirTypeOperatorCall>
typealias FirResolvedQualifierChecker = FirExpressionChecker<FirResolvedQualifier>
typealias FirConstExpressionChecker = FirExpressionChecker<FirConstExpression<*>>
typealias FirCallableReferenceAccessChecker = FirExpressionChecker<FirCallableReferenceAccess>
typealias FirThisReceiverExpressionChecker = FirExpressionChecker<FirThisReceiverExpression>
typealias FirWhileLoopChecker = FirExpressionChecker<FirWhileLoop>
typealias FirThrowExpressionChecker = FirExpressionChecker<FirThrowExpression>
typealias FirDoWhileLoopChecker = FirExpressionChecker<FirDoWhileLoop>
typealias FirArrayOfCallChecker = FirExpressionChecker<FirArrayOfCall>
typealias FirClassReferenceExpressionChecker = FirExpressionChecker<FirClassReferenceExpression>
typealias FirInaccessibleReceiverChecker = FirExpressionChecker<FirInaccessibleReceiverExpression>

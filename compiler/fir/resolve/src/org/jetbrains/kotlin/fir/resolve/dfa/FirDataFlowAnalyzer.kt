/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class FirDataFlowAnalyzer {
    abstract fun getTypeUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): ConeKotlinType?

    // ----------------------------------- Named function -----------------------------------

    abstract fun enterFunction(function: FirFunction<*>)
    abstract fun exitFunction(function: FirFunction<*>): ControlFlowGraph

    // ----------------------------------- Property -----------------------------------

    abstract fun enterProperty(property: FirProperty)
    abstract fun exitProperty(property: FirProperty): ControlFlowGraph

    // ----------------------------------- Block -----------------------------------

    abstract fun enterBlock(block: FirBlock)
    abstract fun exitBlock(block: FirBlock)

    // ----------------------------------- Operator call -----------------------------------

    abstract fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall)
    abstract fun exitOperatorCall(operatorCall: FirOperatorCall)

    // ----------------------------------- Jump -----------------------------------

    abstract fun exitJump(jump: FirJump<*>)

    // ----------------------------------- When -----------------------------------

    abstract fun enterWhenExpression(whenExpression: FirWhenExpression)
    abstract fun enterWhenBranchCondition(whenBranch: FirWhenBranch)
    abstract fun exitWhenBranchCondition(whenBranch: FirWhenBranch)
    abstract fun exitWhenBranchResult(whenBranch: FirWhenBranch)
    abstract fun exitWhenExpression(whenExpression: FirWhenExpression)

    // ----------------------------------- While Loop -----------------------------------

    abstract fun enterWhileLoop(loop: FirLoop)
    abstract fun exitWhileLoopCondition(loop: FirLoop)
    abstract fun exitWhileLoop(loop: FirLoop)

    // ----------------------------------- Do while Loop -----------------------------------

    abstract fun enterDoWhileLoop(loop: FirLoop)
    abstract fun enterDoWhileLoopCondition(loop: FirLoop)
    abstract fun exitDoWhileLoop(loop: FirLoop)

    // ----------------------------------- Try-catch-finally -----------------------------------

    abstract fun enterTryExpression(tryExpression: FirTryExpression)
    abstract fun exitTryMainBlock(tryExpression: FirTryExpression)
    abstract fun enterCatchClause(catch: FirCatch)
    abstract fun exitCatchClause(catch: FirCatch)
    abstract fun enterFinallyBlock(tryExpression: FirTryExpression)
    abstract fun exitFinallyBlock(tryExpression: FirTryExpression)
    abstract fun exitTryExpression(tryExpression: FirTryExpression)

    // ----------------------------------- Resolvable call -----------------------------------

    abstract fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression)
    abstract fun enterFunctionCall(functionCall: FirFunctionCall)
    abstract fun exitFunctionCall(functionCall: FirFunctionCall)
    abstract fun exitConstExpresion(constExpression: FirConstExpression<*>)
    abstract fun exitVariableDeclaration(variable: FirVariable<*>)
    abstract fun exitVariableAssignment(assignment: FirVariableAssignment)
    abstract fun exitThrowExceptionNode(throwExpression: FirThrowExpression)

    // ----------------------------------- Boolean operators -----------------------------------

    abstract fun enterBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression)
    abstract fun exitLeftBinaryAndArgument(binaryLogicExpression: FirBinaryLogicExpression)
    abstract fun exitBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression)
    abstract fun enterBinaryOr(binaryLogicExpression: FirBinaryLogicExpression)
    abstract fun exitLeftBinaryOrArgument(binaryLogicExpression: FirBinaryLogicExpression)
    abstract fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression)

    // ----------------------------------- Annotations -----------------------------------

    abstract fun enterAnnotationCall(annotationCall: FirAnnotationCall)
    abstract fun exitAnnotationCall(annotationCall: FirAnnotationCall)

    // ----------------------------------- Init block -----------------------------------

    abstract fun enterInitBlock(initBlock: FirAnonymousInitializer)
    abstract fun exitInitBlock(initBlock: FirAnonymousInitializer)
}


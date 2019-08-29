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

class DummyFirDataFlowAnalyzer : FirDataFlowAnalyzer() {
    companion object {
        private const val DUMMY = "<DUMMY>"
    }

    override fun getTypeUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): Collection<ConeKotlinType>? {
        return null
    }

    override fun enterFunction(function: FirFunction<*>) {}

    override fun exitFunction(function: FirFunction<*>): ControlFlowGraph? = null

    override fun enterBlock(block: FirBlock) {}

    override fun exitBlock(block: FirBlock) {}

    override fun enterProperty(property: FirProperty) {}

    override fun exitProperty(property: FirProperty) = ControlFlowGraph(DUMMY)

    override fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {}

    override fun exitOperatorCall(operatorCall: FirOperatorCall) {}

    override fun exitJump(jump: FirJump<*>) {}

    override fun enterWhenExpression(whenExpression: FirWhenExpression) {}

    override fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {}

    override fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {}

    override fun exitWhenBranchResult(whenBranch: FirWhenBranch) {}

    override fun exitWhenExpression(whenExpression: FirWhenExpression) {}

    override fun enterWhileLoop(loop: FirLoop) {}

    override fun exitWhileLoopCondition(loop: FirLoop) {}

    override fun exitWhileLoop(loop: FirLoop) {}

    override fun enterDoWhileLoop(loop: FirLoop) {}

    override fun enterDoWhileLoopCondition(loop: FirLoop) {}

    override fun exitDoWhileLoop(loop: FirLoop) {}

    override fun enterTryExpression(tryExpression: FirTryExpression) {}

    override fun exitTryMainBlock(tryExpression: FirTryExpression) {}

    override fun enterCatchClause(catch: FirCatch) {}

    override fun exitCatchClause(catch: FirCatch) {}

    override fun enterFinallyBlock(tryExpression: FirTryExpression) {}

    override fun exitFinallyBlock(tryExpression: FirTryExpression) {}

    override fun exitTryExpression(tryExpression: FirTryExpression) {}

    override fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {}

    override fun enterFunctionCall(functionCall: FirFunctionCall) {}

    override fun exitFunctionCall(functionCall: FirFunctionCall) {}

    override fun exitConstExpresion(constExpression: FirConstExpression<*>) {}

    override fun exitVariableDeclaration(variable: FirVariable<*>) {}

    override fun exitVariableAssignment(assignment: FirVariableAssignment) {}

    override fun exitThrowExceptionNode(throwExpression: FirThrowExpression) {}

    override fun enterBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {}

    override fun exitLeftBinaryAndArgument(binaryLogicExpression: FirBinaryLogicExpression) {}

    override fun exitBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {}

    override fun enterBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {}

    override fun exitLeftBinaryOrArgument(binaryLogicExpression: FirBinaryLogicExpression) {}

    override fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {}

    override fun enterAnnotationCall(annotationCall: FirAnnotationCall) {}

    override fun exitAnnotationCall(annotationCall: FirAnnotationCall) {}

    override fun enterInitBlock(initBlock: FirAnonymousInitializer) {}

    override fun exitInitBlock(initBlock: FirAnonymousInitializer) {}
}
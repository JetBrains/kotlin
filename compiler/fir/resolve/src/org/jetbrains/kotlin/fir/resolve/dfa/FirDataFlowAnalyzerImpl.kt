/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphBuilder
import org.jetbrains.kotlin.fir.resolve.transformers.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.types.ConeKotlinType

class FirDataFlowAnalyzerImpl(transformer: FirBodyResolveTransformer) : FirDataFlowAnalyzer(), BodyResolveComponents by transformer {
    private val graphBuilder = ControlFlowGraphBuilder()

    override fun getTypeUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): ConeKotlinType? {
        return null
    }

    // ----------------------------------- Named function -----------------------------------

    override fun enterFunction(function: FirFunction<*>) {
        graphBuilder.enterFunction(function)
    }

    override fun exitFunction(function: FirFunction<*>): ControlFlowGraph {
        return graphBuilder.exitFunction(function)
    }

    // ----------------------------------- Property -----------------------------------

    override fun enterProperty(property: FirProperty) {
        graphBuilder.enterProperty(property)
    }

    override fun exitProperty(property: FirProperty): ControlFlowGraph {
        val (_, graph) = graphBuilder.exitProperty(property)
        return graph
    }

    // ----------------------------------- Block -----------------------------------

    override fun enterBlock(block: FirBlock) {
        graphBuilder.enterBlock(block)
    }

    override fun exitBlock(block: FirBlock) {
        graphBuilder.exitBlock(block)
    }

    // ----------------------------------- Operator call -----------------------------------

    override fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {
        graphBuilder.exitTypeOperatorCall(typeOperatorCall)
    }

    override fun exitOperatorCall(operatorCall: FirOperatorCall) {
        graphBuilder.exitOperatorCall(operatorCall)
    }

    // ----------------------------------- Jump -----------------------------------

    override fun exitJump(jump: FirJump<*>) {
        graphBuilder.exitJump(jump)
    }

    // ----------------------------------- When -----------------------------------

    override fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression)
    }

    override fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        graphBuilder.enterWhenBranchCondition(whenBranch)
    }

    override fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        graphBuilder.exitWhenBranchCondition(whenBranch)
    }

    override fun exitWhenBranchResult(whenBranch: FirWhenBranch) {
        graphBuilder.exitWhenBranchResult(whenBranch)
    }

    override fun exitWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.exitWhenExpression(whenExpression)
    }

    // ----------------------------------- While Loop -----------------------------------

    override fun enterWhileLoop(loop: FirLoop) {
        graphBuilder.enterWhileLoop(loop)
    }

    override fun exitWhileLoopCondition(loop: FirLoop) {
        graphBuilder.exitWhileLoopCondition(loop)
    }

    override fun exitWhileLoop(loop: FirLoop) {
        graphBuilder.exitWhileLoop(loop)
    }

    // ----------------------------------- Do while Loop -----------------------------------

    override fun enterDoWhileLoop(loop: FirLoop) {
        graphBuilder.enterDoWhileLoop(loop)
    }

    override fun enterDoWhileLoopCondition(loop: FirLoop) {
        graphBuilder.enterDoWhileLoopCondition(loop)
    }

    override fun exitDoWhileLoop(loop: FirLoop) {
        graphBuilder.exitDoWhileLoop(loop)
    }

    // ----------------------------------- Try-catch-finally -----------------------------------

    override fun enterTryExpression(tryExpression: FirTryExpression) {
        graphBuilder.enterTryExpression(tryExpression)
    }

    override fun exitTryMainBlock(tryExpression: FirTryExpression) {
        graphBuilder.exitTryMainBlock(tryExpression)
    }

    override fun enterCatchClause(catch: FirCatch) {
        graphBuilder.enterCatchClause(catch)
    }

    override fun exitCatchClause(catch: FirCatch) {
        graphBuilder.exitCatchClause(catch)
    }

    override fun enterFinallyBlock(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.enterFinallyBlock(tryExpression)
    }

    override fun exitFinallyBlock(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.exitFinallyBlock(tryExpression)
    }

    override fun exitTryExpression(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.exitTryExpression(tryExpression)
    }

    // ----------------------------------- Resolvable call -----------------------------------

    override fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression)
    }

    override fun enterFunctionCall(functionCall: FirFunctionCall) {
        // TODO: add processing in-place lambdas
    }

    override fun exitFunctionCall(functionCall: FirFunctionCall) {
        graphBuilder.exitFunctionCall(functionCall)
    }

    override fun exitConstExpresion(constExpression: FirConstExpression<*>) {
        graphBuilder.exitConstExpresion(constExpression)
    }

    override fun exitVariableDeclaration(variable: FirVariable<*>) {
        graphBuilder.exitVariableDeclaration(variable)
    }

    override fun exitVariableAssignment(assignment: FirVariableAssignment) {
        graphBuilder.exitVariableAssignment(assignment)
    }

    override fun exitThrowExceptionNode(throwExpression: FirThrowExpression) {
        graphBuilder.exitThrowExceptionNode(throwExpression)
    }

    // ----------------------------------- Boolean operators -----------------------------------

    override fun enterBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryAnd(binaryLogicExpression)
    }

    override fun exitLeftBinaryAndArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.exitLeftBinaryAndArgument(binaryLogicExpression)
    }

    override fun exitBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.exitBinaryAnd(binaryLogicExpression)
    }

    override fun enterBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryOr(binaryLogicExpression)
    }

    override fun exitLeftBinaryOrArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.exitLeftBinaryOrArgument(binaryLogicExpression)
    }

    override fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.exitBinaryOr(binaryLogicExpression)
    }

    // ----------------------------------- Annotations -----------------------------------

    override fun enterAnnotationCall(annotationCall: FirAnnotationCall) {
        graphBuilder.enterAnnotationCall(annotationCall)
    }

    override fun exitAnnotationCall(annotationCall: FirAnnotationCall) {
        graphBuilder.exitAnnotationCall(annotationCall)
    }

    // ----------------------------------- Init block -----------------------------------

    override fun enterInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.enterInitBlock(initBlock)
    }

    override fun exitInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.exitInitBlock(initBlock)
    }
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.utils.addToStdlib.runIf

abstract class AbstractOperationFlowBasedChecker : FirControlFlowChecker() {

    fun ControlFlowGraph.collectOperationFlowInfo(session: FirSession): DataFlowInfo {
        val operationFlowOnNodes = mutableMapOf<CFGNode<*>, PersistentOperationFlow>()
        val operationVariableStorage = VariableStorage(session)
        traverse(
            TraverseDirection.Forward,
            ApprovedOperationStatementCollector(session, operationVariableStorage, operationFlowOnNodes)
        )
        return DataFlowInfo(operationVariableStorage, operationFlowOnNodes)
    }

    class ApprovedOperationStatementCollector(
        val session: FirSession,
        val variableStorage: VariableStorage,
        val data: MutableMap<CFGNode<*>, PersistentOperationFlow>
    ) : ControlFlowGraphVisitorVoid() {

        private val logicSystem = object : PersistentOperationLogicSystem(session.typeContext) {

            override fun processUpdatedReceiverVariable(flow: PersistentOperationFlow, variable: RealVariable) =
                throw IllegalStateException("Receiver variable update is not possible for this logic system")

            override fun updateAllReceivers(flow: PersistentOperationFlow) {}
        }

        private val dataFlowResolver = object : DataFlowResolver<PersistentOperationFlow>() {

            override val session: FirSession = this@ApprovedOperationStatementCollector.session
            override val logicSystem: LogicSystem<PersistentOperationFlow> = this@ApprovedOperationStatementCollector.logicSystem
            override val variableStorage: VariableStorage = this@ApprovedOperationStatementCollector.variableStorage
            override val variablesForWhenConditions: MutableMap<WhenBranchConditionExitNode, DataFlowVariable> = mutableMapOf()

            override var CFGNode<*>.flow: PersistentOperationFlow
                get() = data.getValue(this.origin)
                set(value) {
                    data[this.origin] = value
                }
        }

        override fun visitNode(node: CFGNode<*>) {
            node.mergeIncomingFlow()
        }

        override fun visitEqualityOperatorCallNode(node: EqualityOperatorCallNode) {
            super.visitEqualityOperatorCallNode(node)
            dataFlowResolver.processEqualityOperatorCall(node)
        }

        override fun visitTypeOperatorCallNode(node: TypeOperatorCallNode) {
            super.visitTypeOperatorCallNode(node)
            dataFlowResolver.processTypeOperatorCall(node)
        }

        override fun visitBinaryAndExitNode(node: BinaryAndExitNode) {
            super.visitBinaryAndExitNode(node)
            dataFlowResolver.processBinaryBooleanOperator(node.fir, node, isAnd = true)
        }

        override fun visitBinaryOrExitNode(node: BinaryOrExitNode) {
            super.visitBinaryOrExitNode(node)
            dataFlowResolver.processBinaryBooleanOperator(node.fir, node, isAnd = false)
        }

        override fun visitCheckNotNullCallNode(node: CheckNotNullCallNode) {
            super.visitCheckNotNullCallNode(node)
            dataFlowResolver.processCheckNotNullCall(node)
        }

        override fun visitInitBlockEnterNode(node: InitBlockEnterNode) {
            val partOfClassInitializationNode = node.firstPreviousCfgNode as? PartOfClassInitializationNode ?: return
            val prevInitExit = partOfClassInitializationNode.firstPreviousCfgNode as? InitBlockExitNode

            if (prevInitExit != null) {
                dataFlowResolver.processInitBlock(node, prevInitExit)
            } else super.visitInitBlockEnterNode(node)
        }

        override fun visitPostponedLambdaEnterNode(node: PostponedLambdaEnterNode) {
            super.visitPostponedLambdaEnterNode(node)
            dataFlowResolver.processPostponedAnonymousFunction(node)
        }

        override fun visitWhenBranchConditionEnterNode(node: WhenBranchConditionEnterNode) {
            node.mergeIncomingFlow(updateReceivers = true)
            dataFlowResolver.processWhenBranchConditionEnter(node)
        }

        override fun visitWhenBranchConditionExitNode(node: WhenBranchConditionExitNode) {
            super.visitWhenBranchConditionExitNode(node)
            val resultEnterNode = node.followingNodes.find { it is WhenBranchResultEnterNode } as? WhenBranchResultEnterNode ?: return
            dataFlowResolver.processWhenBranchConditionExit(node, resultEnterNode)
        }

        override fun visitWhenBranchResultEnterNode(node: WhenBranchResultEnterNode) {}

        override fun visitWhenExitNode(node: WhenExitNode) {
            val syntheticElseNode = node.previousNodes.find { it is WhenSyntheticElseBranchNode } as? WhenSyntheticElseBranchNode
            if (syntheticElseNode == null) {
                super.visitWhenExitNode(node)
                return
            }

            val previousConditionExitNode = syntheticElseNode.firstPreviousNode as? WhenBranchConditionExitNode
            if (previousConditionExitNode != null) {
                dataFlowResolver.processSyntheticElse(syntheticElseNode, previousConditionExitNode)
            } else syntheticElseNode.mergeIncomingFlow()

            node.mergeIncomingFlow(updateReceivers = true)
        }

        override fun visitCatchClauseEnterNode(node: CatchClauseEnterNode) {
            node.mergeIncomingFlow(updateReceivers = true)
        }

        override fun visitLoopExitNode(node: LoopExitNode) {
            super.visitLoopExitNode(node)
            dataFlowResolver.processLoop(node)
        }

        override fun visitLoopBlockEnterNode(node: LoopBlockEnterNode) {
            val conditionExitNode = node.firstPreviousCfgNode as? LoopConditionExitNode ?: return
            dataFlowResolver.processWhileLoopCondition(conditionExitNode, node)
        }

        override fun visitEnterSafeCallNode(node: EnterSafeCallNode) {
            super.visitEnterSafeCallNode(node)
            dataFlowResolver.processSafeCallEnter(node)
        }

        override fun visitExitSafeCallNode(node: ExitSafeCallNode) {
            super.visitExitSafeCallNode(node)
            dataFlowResolver.processSafeCallExit(node)
        }

        override fun visitBinaryAndEnterRightOperandNode(node: BinaryAndEnterRightOperandNode) {
            val leftExitNode = node.firstPreviousCfgNode as? BinaryAndExitLeftOperandNode ?: return
            dataFlowResolver.processLeftArgumentOfBinaryBooleanOperator(leftExitNode, node, isAnd = true)
        }

        override fun visitBinaryOrEnterRightOperandNode(node: BinaryOrEnterRightOperandNode) {
            val leftExitNode = node.firstPreviousCfgNode as? BinaryOrExitLeftOperandNode ?: return
            dataFlowResolver.processLeftArgumentOfBinaryBooleanOperator(leftExitNode, node, isAnd = false)
        }

        override fun visitElvisRhsEnterNode(node: ElvisRhsEnterNode, data: Nothing?) {
            val leftExitNode = node.firstPreviousCfgNode as? ElvisLhsExitNode ?: return
            val leftIsNotNullNode = leftExitNode.followingNodes.first { it is ElvisLhsIsNotNullNode } as? ElvisLhsIsNotNullNode ?: return
            dataFlowResolver.processElvis(leftExitNode, leftIsNotNullNode, node)
        }

        override fun visitElvisLhsIsNotNullNode(node: ElvisLhsIsNotNullNode, data: Nothing?) {}

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
            super.visitVariableDeclarationNode(node)
            val property = node.fir
            if (!property.isLocal) return
            val initializer = property.initializer ?: return
            dataFlowResolver.processVariableInitialization(node, initializer, property, assignment = null)
        }

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            super.visitVariableAssignmentNode(node)
            val property = (node.fir.lValue as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirProperty ?: return
            if (!property.isLocal && property.isVar) return
            dataFlowResolver.processVariableInitialization(node, node.fir.rValue, property, node.fir)
        }

        override fun visitFunctionCallNode(node: FunctionCallNode) {
            super.visitFunctionCallNode(node)
            if (node.fir.isBooleanNot()) dataFlowResolver.processBooleanNot(node)
        }

        override fun visitUnionFunctionCallArgumentsNode(node: UnionFunctionCallArgumentsNode) {
            dataFlowResolver.processUnionFunctionCallArguments(node)
        }

        override fun visitEnterDefaultArgumentsNode(node: EnterDefaultArgumentsNode, data: Nothing?) {
            node.mergeIncomingFlow(shouldForkFlow = true)
        }

        override fun visitFunctionEnterNode(node: FunctionEnterNode) {
            node.mergeIncomingFlow(shouldForkFlow = true)
        }

        private fun <T : CFGNode<*>> T.mergeIncomingFlow(
            updateReceivers: Boolean = false,
            shouldForkFlow: Boolean = false
        ): T {
            val previousFlows = if (this.isDead) {
                this.previousNodes.mapNotNull { runIf(!this.incomingEdges.getValue(it).isBack) { it.flow } }
            } else {
                this.previousNodes.mapNotNull { prev -> prev.takeIf { this.incomingEdges.getValue(it).usedInDfa }?.flow }
            }

            var flow = logicSystem.joinFlow(previousFlows)
            if (updateReceivers) logicSystem.updateAllReceivers(flow)
            if (shouldForkFlow) flow = logicSystem.forkFlow(flow)

            this.flow = flow
            return this
        }

        private var CFGNode<*>.flow: PersistentOperationFlow
            get() = data.getValue(this.origin)
            set(value) {
                data[this.origin] = value
            }

        private val CFGNode<*>.originFlow: PersistentOperationFlow? get() = data[this.origin]

        private val CFGNode<*>.firstPreviousCfgNode: CFGNode<*>? get() = previousCfgNodes.firstOrNull()

        private val CFGNode<*>.origin: CFGNode<*> get() = if (this is StubNode) firstPreviousNode else this

    }
}
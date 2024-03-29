/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtFakeSourceElementKind.DesugaredAugmentedAssign
import org.jetbrains.kotlin.KtFakeSourceElementKind.DesugaredIncrementOrDecrement
import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.components.KtDataFlowExitPointSnapshot
import org.jetbrains.kotlin.analysis.api.components.KtDataFlowExitPointSnapshot.VariableReassignment
import org.jetbrains.kotlin.analysis.api.components.KtDataFlowInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.unwrap
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.utils.errors.withKtModuleEntry
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirErrorPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirBreakExpression
import org.jetbrains.kotlin.fir.expressions.FirContinueExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirJump
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.AnonymousObjectExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.DelegateExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisLhsExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitNodeMarker
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitSafeCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitValueParameterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LocalClassExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.PostponedLambdaExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.SmartCastExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.StubNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchResultExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenSubjectExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.commonSuperTypeOrNull
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.ArrayList
import kotlin.math.sign

@OptIn(KtAnalysisNonPublicApi::class)
internal class KtFirDataFlowInfoProvider(override val analysisSession: KtFirAnalysisSession) : KtDataFlowInfoProvider() {
    override fun getExitPointSnapshot(statements: List<KtExpression>): KtDataFlowExitPointSnapshot {
        val firResolveSession = analysisSession.firResolveSession

        val parent = getCommonParent(statements)
        val firParent = parent.parentsWithSelf
            .filterIsInstance<KtElement>()
            .firstNotNullOf { it.getOrBuildFir(firResolveSession) }

        val unwrappedStatements = statements.map { it.unwrap() }

        val statementSearcher = FirStatementSearcher(unwrappedStatements)
        firParent.accept(statementSearcher)

        val firStatements = unwrappedStatements.map { statementSearcher[it] ?: it.getOrBuildFirOfType<FirElement>(firResolveSession) }

        val collector = FirElementCollector()
        firStatements.forEach { it.accept(collector) }

        val firValuedReturnExpressions = collector.firReturnExpressions.filter { !it.result.resolvedType.isUnit }

        val firDefaultStatementCandidate = firStatements.last()
        val defaultExpressionInfo = computeDefaultExpression(statements, firDefaultStatementCandidate, firValuedReturnExpressions)

        val firEscapingCandidates = buildSet<FirElement> {
            add(firDefaultStatementCandidate)
            addAll(collector.firReturnExpressions)
            addAll(collector.firBreakExpressions)
            addAll(collector.firContinueExpressions)
        }

        val hasEscapingJumps = computeHasEscapingJumps(statements.first(), firStatements.first(), firEscapingCandidates)

        val loopJumpExpressions = ArrayList<KtExpression>(collector.firBreakExpressions.size + collector.firContinueExpressions.size)
        collector.firBreakExpressions.mapNotNullTo(loopJumpExpressions) { it.psi as? KtExpression }
        collector.firContinueExpressions.mapNotNullTo(loopJumpExpressions) { it.psi as? KtExpression }

        return KtDataFlowExitPointSnapshot(
            defaultExpressionInfo = defaultExpressionInfo,
            valuedReturnExpressions = firValuedReturnExpressions.mapNotNull { it.psi as? KtExpression },
            returnValueType = computeReturnType(firValuedReturnExpressions),
            loopJumpExpressions = loopJumpExpressions,
            hasJumps = collector.hasJumps,
            hasEscapingJumps = hasEscapingJumps,
            hasMultipleJumpKinds = collector.hasMultipleJumpKinds,
            hasMultipleJumpTargets = collector.hasMultipleJumpTargets,
            variableReassignments = collector.variableReassignments
        )
    }

    private fun getCommonParent(statements: List<KtElement>): KtElement {
        require(statements.isNotEmpty())

        val parent = statements[0].parent as KtElement

        for (i in 1..<statements.size) {
            require(statements[i].parent == parent)
        }

        return parent
    }

    private fun computeDefaultExpression(
        statements: List<KtExpression>,
        firDefaultStatement: FirElement,
        firValuedReturnExpressions: List<FirReturnExpression>
    ): KtDataFlowExitPointSnapshot.DefaultExpressionInfo? {
        val defaultExpressionFromPsi = statements.last()

        if (firDefaultStatement in firValuedReturnExpressions) {
            return null
        }

        when (firDefaultStatement) {
            !is FirExpression,
            is FirJump<*>,
            is FirThrowExpression,
            is FirResolvedQualifier,
            is FirUnitExpression,
            is FirErrorExpression -> {
                return null
            }

            is FirBlock -> {
                if (firDefaultStatement.resolvedType.isUnit) {
                    return null
                }
            }
        }

        @Suppress("USELESS_IS_CHECK") // K2 warning suppression, TODO: KT-62472
        require(firDefaultStatement is FirExpression)

        val defaultExpressionFromFir = firDefaultStatement.psi as? KtExpression ?: return null

        if (!PsiTreeUtil.isAncestor(defaultExpressionFromFir, defaultExpressionFromPsi, false)) {
            // In certain cases, expressions might be different in PSI and FIR sources.
            // E.g., in 'foo.<expr>bar()</expr>', there is no FIR expression that corresponds to the 'bar()' KtCallExpression.
            return null
        }

        val defaultConeType = firDefaultStatement.resolvedType
        if (defaultConeType.isNothing) {
            return null
        }

        val defaultType = defaultConeType.toKtType()
        return KtDataFlowExitPointSnapshot.DefaultExpressionInfo(defaultExpressionFromPsi, defaultType)
    }

    private fun computeReturnType(firReturnExpressions: List<FirReturnExpression>): KtType? {
        val coneTypes = ArrayList<ConeKotlinType>(firReturnExpressions.size)

        for (firReturnExpression in firReturnExpressions) {
            val coneType = firReturnExpression.result.resolvedType
            if (coneType.isUnit) {
                return null
            }

            coneTypes.add(coneType)
        }

        return analysisSession.useSiteSession.typeContext.commonSuperTypeOrNull(coneTypes)?.toKtType()
    }

    private fun computeHasEscapingJumps(anchor: KtElement, firAnchor: FirElement, firTargets: Set<FirElement>): Boolean {
        if (firTargets.size < 2) {
            // There should be both a default expression and some kind of jump
            return false
        }

        val graph = findControlFlowGraph(anchor, firAnchor)
            ?: errorWithAttachment("Cannot find a control flow graph for element") {
                withKtModuleEntry("module", analysisSession.useSiteModule)
                withPsiEntry("anchor", anchor)
                withFirEntry("firAnchor", firAnchor)
            }

        val exitNodes = HashMap<FirElement, List<CFGNode<*>>>()
        graph.collectExitNodes(firTargets, exitNodes)

        return exitNodes.values.distinct().size > 1
    }

    private fun findControlFlowGraph(anchor: KtElement, firAnchor: FirElement): ControlFlowGraph? {
        val parentDeclarations = anchor.parentsOfType<KtDeclaration>(withSelf = false)
        for (parentDeclaration in parentDeclarations) {
            val parentFirDeclaration = parentDeclaration.getOrBuildFir(analysisSession.firResolveSession)
            if (parentFirDeclaration is FirControlFlowGraphOwner) {
                val graph = parentFirDeclaration.controlFlowGraphReference?.controlFlowGraph
                if (graph != null && firAnchor in graph) {
                    return graph
                }
            }
        }

        return null
    }

    private fun ControlFlowGraph.collectExitNodes(firTargets: Set<FirElement>, exitNodes: MutableMap<FirElement, List<CFGNode<*>>>) {
        for (node in nodes) {
            val fir = node.fir
            if (fir in firTargets) {
                // This intentionally replaces existing values, in case if there are multiple nodes with the same FirElement.
                // Here we look for exits, so we are interested in the last matching node.
                exitNodes[fir] = node.followingNodes
                    .filter { it !is StubNode }
                    .map { it.unwrap() }
                    .distinct()
                    .sortedBy { it.id }
            }
            if (node is CFGNodeWithSubgraphs<*>) {
                for (subGraph in node.subGraphs) {
                    subGraph.collectExitNodes(firTargets, exitNodes)
                }
            }
        }
    }

    private fun CFGNode<*>.unwrap(): CFGNode<*> {
        var current = this

        while (current.isExitNode()) {
            val following = current.followingNodes
            if (following.size == 1) {
                current = following.first()
            } else {
                break
            }
        }

        return current
    }

    private fun CFGNode<*>.isExitNode(): Boolean {
        return when (this) {
            is ExitNodeMarker, is ExitValueParameterNode, is WhenSubjectExpressionExitNode, is AnonymousObjectExpressionExitNode,
            is SmartCastExpressionExitNode, is PostponedLambdaExitNode, is DelegateExpressionExitNode, is WhenBranchResultExitNode,
            is ElvisExitNode, is ExitSafeCallNode, is LocalClassExitNode, is ElvisLhsExitNode -> {
                true
            }
            else -> {
                false
            }
        }
    }

    private operator fun ControlFlowGraph.contains(fir: FirElement): Boolean {
        for (node in nodes) {
            if (node.fir == fir) {
                return true
            }
            if (node is CFGNodeWithSubgraphs<*> && node.subGraphs.any { fir in it }) {
                return true
            }
        }

        return false
    }

    /**
     * This class, unlike 'getOrBuildFirOfType()', tries to find topmost FIR elements.
     * This is useful when PSI expressions get wrapped in the FIR tree, such as implicit return expressions from lambdas.
     */
    private inner class FirStatementSearcher(statements: List<KtExpression>) : FirDefaultVisitorVoid() {
        private val statements = statements.toHashSet()

        private val mapping = LinkedHashMap<KtExpression, FirElement?>()
        private var unmappedCount = statements.size

        operator fun get(statement: KtExpression): FirElement? {
            return mapping[statement]
        }

        override fun visitElement(element: FirElement) {
            val psi = element.psi

            if (psi is KtExpression && psi in statements) {
                mapping.computeIfAbsent(psi) { _ ->
                    unmappedCount -= 1
                    element
                }
            }

            if (unmappedCount > 0) {
                element.acceptChildren(this)
            }
        }
    }

    private inner class FirElementCollector : FirDefaultVisitorVoid() {
        val hasJumps: Boolean
            get() = firReturnTargets.isNotEmpty() || firLoopJumpTargets.isNotEmpty()

        val hasMultipleJumpKinds: Boolean
            get() = (firReturnExpressions.size.sign + firBreakExpressions.size.sign + firContinueExpressions.size.sign) > 1

        val hasMultipleJumpTargets: Boolean
            get() = (firReturnTargets.size + firLoopJumpTargets.size) > 1

        val variableReassignments = mutableListOf<VariableReassignment>()

        val firReturnExpressions = mutableListOf<FirReturnExpression>()
        val firBreakExpressions = mutableListOf<FirBreakExpression>()
        val firContinueExpressions = mutableListOf<FirContinueExpression>()

        private val firReturnTargets = mutableSetOf<FirFunction>()
        private val firLoopJumpTargets = mutableSetOf<FirLoop>()

        private val firFunctionDeclarations = mutableSetOf<FirFunction>()
        private val firLoopStatements = mutableSetOf<FirLoop>()

        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) = visitFunction(anonymousFunction)
        override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) = visitFunction(propertyAccessor)
        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) = visitFunction(simpleFunction)
        override fun visitErrorFunction(errorFunction: FirErrorFunction) = visitFunction(errorFunction)
        override fun visitConstructor(constructor: FirConstructor) = visitFunction(constructor)
        override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor) = visitFunction(errorPrimaryConstructor)

        override fun visitFunction(function: FirFunction) {
            firFunctionDeclarations.add(function)
            super.visitFunction(function)
        }

        override fun visitLoop(loop: FirLoop) {
            firLoopStatements.add(loop)
            super.visitLoop(loop)
        }

        override fun visitReturnExpression(returnExpression: FirReturnExpression) {
            if (returnExpression.target.labeledElement !in firFunctionDeclarations) {
                firReturnExpressions.add(returnExpression)
                firReturnTargets.add(returnExpression.target.labeledElement)
            }
            super.visitReturnExpression(returnExpression)
        }

        override fun visitBreakExpression(breakExpression: FirBreakExpression) {
            if (breakExpression.target.labeledElement !in firLoopStatements) {
                firBreakExpressions.add(breakExpression)
                firLoopJumpTargets.add(breakExpression.target.labeledElement)
            }
            super.visitBreakExpression(breakExpression)
        }

        override fun visitContinueExpression(continueExpression: FirContinueExpression) {
            if (continueExpression.target.labeledElement !in firLoopStatements) {
                firContinueExpressions.add(continueExpression)
                firLoopJumpTargets.add(continueExpression.target.labeledElement)
            }
            super.visitContinueExpression(continueExpression)
        }

        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
            val firVariableSymbol = variableAssignment.lValue.toResolvedCallableSymbol(analysisSession.useSiteSession)
            val expression = variableAssignment.psi as? KtExpression

            if (firVariableSymbol is FirVariableSymbol<*> && firVariableSymbol.fir.isLocalMember && expression != null) {
                val variableSymbol = analysisSession.firSymbolBuilder.variableLikeBuilder.buildVariableLikeSymbol(firVariableSymbol)
                val reassignment = VariableReassignment(expression, variableSymbol, variableAssignment.isAugmented())
                variableReassignments.add(reassignment)
            }

            super.visitVariableAssignment(variableAssignment)
        }

        private fun FirVariableAssignment.isAugmented(): Boolean {
            val targetSource = lValue.source
            if (targetSource != null) {
                when (targetSource.kind) {
                    is DesugaredAugmentedAssign, is DesugaredIncrementOrDecrement -> return true
                    else -> {}
                }
            }

            return false
        }
    }

    private fun ConeKotlinType.toKtType(): KtType {
        return analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)
    }
}
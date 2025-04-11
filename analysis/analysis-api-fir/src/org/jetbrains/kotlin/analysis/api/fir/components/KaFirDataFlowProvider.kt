/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtFakeSourceElementKind.DesugaredAugmentedAssign
import org.jetbrains.kotlin.KtFakeSourceElementKind.DesugaredIncrementOrDecrement
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.components.KaDataFlowExitPointSnapshot.DefaultExpressionInfo
import org.jetbrains.kotlin.analysis.api.components.KaDataFlowExitPointSnapshot.VariableReassignment
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.unwrap
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSmartCastInfo
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.utils.errors.withKaModuleEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.collectUseSiteContainers
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.sign

internal class KaFirDataFlowProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaDataFlowProvider, KaFirSessionComponent {
    override val KtExpression.smartCastInfo: KaSmartCastInfo?
        get() = withValidityAssertion {
            val firSmartCastExpression = getMatchingFirExpressionWithSmartCast(this) ?: return null
            val type = firSmartCastExpression.smartcastType.coneType.asKtType()
            return KaBaseSmartCastInfo(type, firSmartCastExpression.isStable)
        }

    override val KtExpression.implicitReceiverSmartCasts: Collection<KaImplicitReceiverSmartCast>
        get() = withValidityAssertion {
            val firQualifiedExpression = getMatchingFirQualifiedAccessExpression(this) ?: return emptyList()

            listOfNotNull(
                createImplicitReceiverSmartCast(firQualifiedExpression, KaImplicitReceiverSmartCastKind.DISPATCH),
                createImplicitReceiverSmartCast(firQualifiedExpression, KaImplicitReceiverSmartCastKind.EXTENSION),
            )
        }

    private fun getMatchingFirExpressionWithSmartCast(expression: KtExpression): FirSmartCastExpression? {
        if (!expression.isExplicitSmartCastInfoTarget) return null

        val possibleFunctionCall = expression.getPossiblyQualifiedCallExpressionForCallee() ?: expression

        return when (val firExpression = possibleFunctionCall.getOrBuildFir(analysisSession.resolutionFacade)) {
            is FirSmartCastExpression -> firExpression
            is FirSafeCallExpression -> firExpression.selector as? FirSmartCastExpression
            is FirImplicitInvokeCall -> firExpression.explicitReceiver as? FirSmartCastExpression
            else -> null
        }
    }

    private val KtExpression.isExplicitSmartCastInfoTarget: Boolean
        get() {
            // we want to handle only most top-level parenthesised expressions
            if (parent is KtParenthesizedExpression) return false

            // expressions like `|foo.bar()|` or `|foo?.baz()|` are ignored
            if (this is KtQualifiedExpression && selectorExpression is KtCallExpression) return false

            // expressions like `foo.|bar|` or `foo?.|baz|` are ignored
            if (this is KtNameReferenceExpression && getQualifiedExpressionForSelector() != null) return false

            // only those types of expressions are supported
            return this is KtQualifiedExpression ||
                    this is KtNameReferenceExpression ||
                    this is KtParenthesizedExpression
        }

    private fun getMatchingFirQualifiedAccessExpression(expression: KtExpression): FirQualifiedAccessExpression? {
        if (!expression.isImplicitSmartCastInfoTarget) return null

        val wholeExpression = expression.getOperationExpressionForOperationReference()
            ?: expression.getPossiblyQualifiedCallExpressionForCallee()
            ?: expression.getQualifiedExpressionForSelector()
            ?: expression

        return when (val firExpression = wholeExpression.getOrBuildFir(analysisSession.resolutionFacade)) {
            is FirQualifiedAccessExpression -> firExpression
            is FirSafeCallExpression -> firExpression.selector as? FirQualifiedAccessExpression
            is FirSmartCastExpression -> firExpression.originalExpression as? FirQualifiedAccessExpression
            else -> null
        }
    }

    private fun KtExpression.getPossiblyQualifiedCallExpressionForCallee(): KtExpression? {
        val expressionParent = this.parent

        return if (expressionParent is KtCallExpression && expressionParent.calleeExpression == this) {
            expressionParent.getQualifiedExpressionForSelectorOrThis()
        } else {
            null
        }
    }

    private fun KtExpression.getOperationExpressionForOperationReference(): KtOperationExpression? =
        (this as? KtOperationReferenceExpression)?.parent as? KtOperationExpression

    private val KtExpression.isImplicitSmartCastInfoTarget: Boolean
        get() = this is KtNameReferenceExpression || this is KtOperationReferenceExpression

    private fun createImplicitReceiverSmartCast(
        firExpression: FirQualifiedAccessExpression,
        kind: KaImplicitReceiverSmartCastKind,
    ): KaImplicitReceiverSmartCast? {
        val receiver = when (kind) {
            KaImplicitReceiverSmartCastKind.DISPATCH -> firExpression.dispatchReceiver
            KaImplicitReceiverSmartCastKind.EXTENSION -> firExpression.extensionReceiver
        }

        if (receiver == null || receiver == firExpression.explicitReceiver) return null
        if (!receiver.isStableSmartcast()) return null

        val type = receiver.resolvedType.asKtType()
        return KaBaseImplicitReceiverSmartCast(type, kind)
    }

    override fun computeExitPointSnapshot(statements: List<KtExpression>): KaDataFlowExitPointSnapshot = withValidityAssertion {
        val firStatements = computeStatements(statements)

        val collector = FirElementCollector()
        firStatements.forEach { it.accept(collector) }

        val firValuedReturnExpressions = collector.firReturnExpressions.filter { !it.result.resolvedType.isUnit }

        val defaultStatement = statements.last()
        val firDefaultStatement = firStatements.last()
        val defaultExpressionInfo = computeDefaultExpression(defaultStatement, firDefaultStatement, firValuedReturnExpressions)

        val graphIndex = ControlFlowGraphIndex {
            getControlFlowGraph(anchor = statements.first(), firStatements = firStatements)
        }

        val jumpExpressions = buildList {
            fun add(expressions: List<FirElement>) {
                expressions.mapNotNullTo(this) { it.psi as? KtExpression }
            }

            add(collector.firReturnExpressions)
            add(collector.firBreakExpressions)
            add(collector.firContinueExpressions)
        }

        return KaDataFlowExitPointSnapshot(
            defaultExpressionInfo = defaultExpressionInfo,
            valuedReturnExpressions = firValuedReturnExpressions.mapNotNull { it.psi as? KtExpression },
            returnValueType = computeReturnType(firValuedReturnExpressions),
            jumpExpressions = jumpExpressions,
            hasJumps = collector.hasJumps,
            hasEscapingJumps = graphIndex.computeHasEscapingJumps(firDefaultStatement, collector),
            hasMultipleJumpKinds = collector.hasMultipleJumpKinds,
            hasMultipleJumpTargets = graphIndex.computeHasMultipleJumpTargets(collector),
            variableReassignments = collector.variableReassignments
        )
    }

    private fun computeStatements(statements: List<KtExpression>): List<FirElement> {
        val firParent = computeCommonParent(statements)

        val firStatements = statements.map { it.unwrap().getOrBuildFirOfType<FirElement>(resolutionFacade) }
        val pathSearcher = FirElementPathSearcher(firStatements)
        firParent.accept(pathSearcher)

        return firStatements.map { unwrapLeaf(pathSearcher[it] ?: it) }
    }

    private fun unwrapLeaf(firLeaf: FirElement): FirElement {
        if (firLeaf is FirBlock && firLeaf.statements.size == 1) {
            val firStatement = firLeaf.statements[0]
            if (firStatement is FirExpression && firStatement.resolvedType == firLeaf.resolvedType) {
                // Trivial blocks might not appear in the CFG, so here we include their content
                return firStatement
            }
        }

        return firLeaf
    }

    private fun computeCommonParent(statements: List<KtElement>): FirElement {
        require(statements.isNotEmpty())

        val parent = statements[0].parent as KtElement

        for (i in 1..<statements.size) {
            require(statements[i].parent == parent)
        }

        val firContainer = collectUseSiteContainers(parent, resolutionFacade)?.lastOrNull()
        if (firContainer != null) {
            firContainer.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return firContainer
        }

        return parent.parentsWithSelf
            .filterIsInstance<KtElement>()
            .firstNotNullOf { it.getOrBuildFir(resolutionFacade) }
    }

    private fun computeDefaultExpression(
        defaultStatement: KtExpression,
        firDefaultStatement: FirElement,
        firValuedReturnExpressions: List<FirReturnExpression>
    ): DefaultExpressionInfo? {
        if (firDefaultStatement in firValuedReturnExpressions) {
            return null
        }

        val defaultConeType = computeOrdinaryDefaultType(defaultStatement, firDefaultStatement)
            ?: computeOperationDefaultType(defaultStatement)
            ?: computeAssignmentTargetDefaultType(defaultStatement, firDefaultStatement)

        if (defaultConeType == null || defaultConeType.isNothing) {
            return null
        }

        return DefaultExpressionInfo(defaultStatement, defaultConeType.toKtType())
    }

    private fun computeOrdinaryDefaultType(defaultStatement: KtExpression, firDefaultStatement: FirElement): ConeKotlinType? {
        when (firDefaultStatement) {
            !is FirExpression,
            is FirJump<*>,
            is FirThrowExpression,
            is FirUnitExpression,
            is FirErrorExpression -> {
                return null
            }

            is FirBlock,
            is FirResolvedQualifier -> {
                if (firDefaultStatement.resolvedType.isUnit) {
                    return null
                }
            }
        }

        val defaultStatementFromFir = firDefaultStatement.psi as? KtExpression ?: return null

        if (!PsiTreeUtil.isAncestor(defaultStatementFromFir, defaultStatement.deparenthesize(), false)) {
            // In certain cases, expressions might be different in PSI and FIR sources.
            // E.g., in 'foo.<expr>bar()</expr>', there is no FIR expression that corresponds to the 'bar()' KtCallExpression.
            return null
        }

        return firDefaultStatement.resolvedType
    }

    private fun computeOperationDefaultType(defaultStatement: KtExpression): ConeKotlinType? {
        val binaryExpression = (defaultStatement as? KtOperationReferenceExpression)?.parent as? KtBinaryExpression ?: return null
        val expressionCall = binaryExpression.getOrBuildFirSafe<FirQualifiedAccessExpression>(resolutionFacade) ?: return null
        val receiverCall = expressionCall.explicitReceiver as? FirQualifiedAccessExpression ?: return null

        return receiverCall.resolvedType
    }

    private fun computeAssignmentTargetDefaultType(defaultStatement: KtExpression, firDefaultStatement: FirElement): ConeKotlinType? {
        if (firDefaultStatement is FirVariableAssignment && firDefaultStatement.lValue.psi == defaultStatement) {
            return computeOrdinaryDefaultType(defaultStatement, firDefaultStatement.lValue)
        }

        return null
    }

    private fun computeReturnType(firReturnExpressions: List<FirReturnExpression>): KaType? {
        val coneTypes = ArrayList<ConeKotlinType>(firReturnExpressions.size)

        for (firReturnExpression in firReturnExpressions) {
            val coneType = firReturnExpression.result.resolvedType
            if (coneType.isUnit) {
                return null
            }

            coneTypes.add(coneType)
        }

        return analysisSession.firSession.typeContext.commonSuperTypeOrNull(coneTypes)?.toKtType()
    }

    private fun ControlFlowGraphIndex.computeHasEscapingJumps(firDefaultStatement: FirElement, collector: FirElementCollector): Boolean {
        val firTargets = buildSet<FirElement> {
            add(firDefaultStatement)
            addAll(collector.firReturnExpressions)
            addAll(collector.firBreakExpressions)
            addAll(collector.firContinueExpressions)
        }

        return hasMultipleExitPoints(firTargets)
    }

    private fun ControlFlowGraphIndex.computeHasMultipleJumpTargets(collector: FirElementCollector): Boolean {
        val firTargets = buildSet<FirElement> {
            addAll(collector.firReturnExpressions)
            addAll(collector.firBreakExpressions)
            addAll(collector.firContinueExpressions)
        }

        return hasMultipleExitPoints(firTargets)
    }

    private fun getControlFlowGraph(anchor: KtElement, firStatements: List<FirElement>): ControlFlowGraph {
        return findControlFlowGraph(anchor, firStatements)
            ?: errorWithAttachment("Cannot find a control flow graph for element") {
                withKaModuleEntry("module", analysisSession.useSiteModule)
                withPsiEntry("anchor", anchor)
                withFirEntry("firAnchor", firStatements.last())
            }
    }

    private fun findControlFlowGraph(anchor: KtElement, firStatements: List<FirElement>): ControlFlowGraph? {
        /**
         * Not all expressions appear in the [ControlFlowGraph].
         * Still, if we find at least some of them, it's very unlikely that we will ever find a better graph.
         */
        val firCandidates = buildSet {
            fun addCandidate(firCandidate: FirElement) {
                add(firCandidate)

                if (firCandidate is FirBlock) {
                    firCandidate.statements.forEach(::addCandidate)
                }
            }

            firStatements.forEach(::addCandidate)
        }

        val parentDeclarations = anchor.parentsOfType<KtDeclaration>(withSelf = true)
        for (parentDeclaration in parentDeclarations) {
            val parentFirDeclaration = parentDeclaration.resolveToFirSymbol(resolutionFacade, FirResolvePhase.BODY_RESOLVE).fir
            if (parentFirDeclaration is FirControlFlowGraphOwner) {
                val graph = parentFirDeclaration.controlFlowGraphReference?.controlFlowGraph
                if (graph != null && graph.contains(firCandidates)) {
                    return graph
                }
            }
        }

        return null
    }

    private fun ControlFlowGraphIndex.hasMultipleExitPoints(firTargets: Set<FirElement>): Boolean {
        if (firTargets.size < 2) {
            return false
        }

        val exitPoints = firTargets
            .mapNotNull { findLast(it) }
            .flatMap { node ->
                node.followingNodes
                    .filter { it !is StubNode }
                    .map { it.unwrap() }
                    .distinct()
                    .sortedBy { it.id }
            }.distinct()

        return exitPoints.size > 1
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

    /**
     * Returns `true` if the control graph contains at least one of the [firCandidates].
     */
    private fun ControlFlowGraph.contains(firCandidates: Set<FirElement>): Boolean {
        for (node in nodes) {
            if (node.fir in firCandidates) {
                return true
            }
            if (node is CFGNodeWithSubgraphs<*> && node.subGraphs.any { it.contains(firCandidates) }) {
                return true
            }
        }

        return false
    }

    private class FirElementPathSearcher(statements: Collection<FirElement>) : FirDefaultVisitorVoid() {
        private companion object {
            val FORBIDDEN_FAKE_SOURCE_KINDS: Set<KtFakeSourceElementKind> = setOf(
                KtFakeSourceElementKind.WhenCondition,
                KtFakeSourceElementKind.SingleExpressionBlock
            )
        }

        private val statements = statements.toHashSet()

        private val mapping = HashMap<FirElement, Optional<FirElement>>()
        private var unmappedCount = statements.size

        private val stack = ArrayDeque<FirElement>()

        operator fun get(fir: FirElement): FirElement? {
            return mapping[fir]?.getOrNull()
        }

        override fun visitElement(element: FirElement) {
            withElement(element) {
                if (element in statements) {
                    // The leaf is in mapping, but its value is still 'null'
                    mapping.computeIfAbsent(element) { _ ->
                        unmappedCount -= 1
                        // Here we intentionally use the 'Optional' wrapper to make 'computeIfAbsent' work smoothly
                        Optional.ofNullable(computeTarget(element))
                    }
                }

                if (unmappedCount > 0) {
                    element.acceptChildren(this)
                }
            }
        }

        private fun computeTarget(element: FirElement): FirElement? {
            val psi = element.psi
            return stack.firstOrNull { it.psi == psi && isAppropriateTarget(it) }
        }

        private fun isAppropriateTarget(element: FirElement): Boolean {
            if (element !is FirStatement && element !is FirReference) {
                return false
            }

            val source = element.source
            val isImplicitWhenSubjectVariable = element is FirProperty && element.isImplicitWhenSubjectVariable
            return source?.kind !in FORBIDDEN_FAKE_SOURCE_KINDS && !isImplicitWhenSubjectVariable
        }

        private inline fun withElement(element: FirElement, block: () -> Unit) {
            stack.addLast(element)
            try {
                block()
            } finally {
                stack.removeLast()
            }
        }
    }

    private inner class FirElementCollector : FirDefaultVisitorVoid() {
        val hasJumps: Boolean
            get() = firReturnTargets.isNotEmpty() || firLoopJumpTargets.isNotEmpty()

        val hasMultipleJumpKinds: Boolean
            get() = (firReturnExpressions.size.sign + firBreakExpressions.size.sign + firContinueExpressions.size.sign) > 1

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
            val firVariableSymbol = variableAssignment.lValue.toResolvedCallableSymbol(analysisSession.firSession)
            val expression = variableAssignment.psi as? KtExpression

            if (firVariableSymbol is FirVariableSymbol<*> && firVariableSymbol.fir.isLocalMember && expression != null) {
                val variableSymbol = analysisSession.firSymbolBuilder.variableBuilder.buildVariableSymbol(firVariableSymbol)
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

    private fun ConeKotlinType.toKtType(): KaType {
        return analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)
    }
}

private class ControlFlowGraphIndex(graphProvider: () -> ControlFlowGraph) {
    private val mapping: Map<FirElement, List<CFGNode<*>>> by lazy {
        val result = HashMap<FirElement, MutableList<CFGNode<*>>>()

        fun addGraph(graph: ControlFlowGraph) {
            for (node in graph.nodes) {
                result.getOrPut(node.fir) { SmartList() }.add(node)

                if (node is CFGNodeWithSubgraphs<*>) {
                    node.subGraphs.forEach(::addGraph)
                }
            }
        }

        addGraph(graphProvider())

        return@lazy result
    }

    /**
     * Find the last node in a graph (or its subgraphs) that point to the given [fir] element.
     */
    fun findLast(fir: FirElement): CFGNode<*>? {
        val directNodes = mapping[fir]
        if (directNodes != null) {
            return directNodes.last()
        }

        if (fir is FirBlock) {
            return fir.statements
                .asReversed()
                .firstNotNullOfOrNull(::findLast)
        }

        return null
    }
}

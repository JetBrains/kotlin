/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.nearestNonInPlaceGraph
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isIterator
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.isDelegatedProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.SpecialNames

/**
 * A checker for tracking local variable assignments which are unused.
 *
 * ## In-place Assignments
 *
 * Using the control-flow graph of a declaration, this checker tracks all local variable assignments
 * and marks them as used or unused at each variable access. When assignments happen in-place with
 * variable access, only the most recent assignments are considered used. When code branches and
 * joins, it is possible to have multiple recent assignments.
 *
 * ## Captured Assignments
 *
 * Consider the following graph:
 * ```
 *       |
 *       * < (1)
 *       |\
 *       | \
 * (2) > *  * < (3)
 *       |  |
 * ```
 * Where nodes (1), (2), and (3) represent parts of the following code:
 * ```kotlin
 * fun example() {
 *     // (1)
 *     invokeLater { /* (3) */ }
 *     // (2)
 * }
 * ```
 *
 * Given some local variables defined in (1) and that the execution order of (2) and (3) is
 * undetermined, assignments within (2) could be visible by accessors in (3). Similarly, assignments
 * within (3) could be visible by accessors in (2). This means that future assignments to local
 * variables need to flow into non-in-place parts of the control-flow graph.
 *
 * ## Implementation Details
 *
 * Gathering the *visible assignments* for each node within the control-flow graph works in two
 * phases:
 * 1. Back-propagate ***all*** variable assignments to previous nodes as captured assignments.
 * 2. Forward-propagate the ***most recent in-place*** variable assignments and combine this with
 * ***all captured*** assignments from non-in-place subgraphs and parent graphs from step 1.
 *
 * Now that the visible assignments are known for each node, traverse the graph a final time to find
 * each variable access node and mark the assignments visible to that node as used. Any unmarked
 * assignments are considered unused.
 */
object UnusedVariableAssignmentChecker : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun analyze(data: VariableInitializationInfoData) {
        @Suppress("UNCHECKED_CAST")
        val properties = data.properties as Set<FirPropertySymbol>
        val ownData = VariableAssignmentData(properties)
        data.graph.traverse(AddAllWrites(ownData))
        if (ownData.unreadWrites.isNotEmpty()) {
            val capturedWrites = data.graph.traverseToFixedPoint(FindCapturedWrites(properties))
            ownData.writesByNode = data.graph.traverseToFixedPoint(FindVisibleWrites(capturedWrites, properties))
        }
        data.graph.traverse(RemoveVisibleWrites(ownData))

        val variablesWithUnobservedWrites = mutableSetOf<FirPropertySymbol>()
        for (statement in ownData.unreadWrites) {
            if (statement is FirVariableAssignment) {
                val variableSymbol = statement.calleeReference?.toResolvedPropertySymbol() ?: continue
                variablesWithUnobservedWrites.add(variableSymbol)
                if (!variableSymbol.isDelegatedProperty) {
                    // Writes to delegated properties may not be local and may be read somewhere else.
                    // TODO, KT-59831: report case like "a += 1" where `a` `doesn't writes` different way (special for Idea)
                    reporter.reportOn(statement.lValue.source, FirErrors.ASSIGNED_VALUE_IS_NEVER_READ)
                }
            } else if (statement is FirProperty) {
                if (statement.symbol !in ownData.variablesWithoutReads && !statement.symbol.ignoreWarnings) {
                    reporter.reportOn(statement.initializer?.source, FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT)
                }
            }
        }

        for ((symbol, fir) in ownData.variablesWithoutReads) {
            if (symbol.ignoreWarnings) continue
            if ((fir.initializer as? FirFunctionCall)?.isIterator == true || fir.isCatchParameter == true) continue
            val error = when {
                symbol in variablesWithUnobservedWrites -> when (symbol.isDelegatedProperty) {
                    false -> FirErrors.VARIABLE_NEVER_READ
                    true -> null // Delegated properties should only report when completely unused.
                }
                else -> FirErrors.UNUSED_VARIABLE
            }
            if (error != null) reporter.reportOn(fir.source, error)
        }
    }

    private val FirPropertySymbol.ignoreWarnings: Boolean
        get() = name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR ||
                source == null ||
                // if <receiver> variable is reported as unused,
                // then the assignment itself is a dead code because of its RHS expression,
                // which will be eventually reported
                source?.kind is KtFakeSourceElementKind.DesugaredAugmentedAssign ||
                source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION ||
                initializerSource?.kind == KtFakeSourceElementKind.DesugaredForLoop

    private class AddAllWrites(private val data: VariableAssignmentData) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
            // `name.isSpecial` checks in this analysis are required to avoid cases like KT-72164:
            // In cases of incorrect syntactic constructions, the analysis is still performed.
            // However, some invalid constructions can be considered as variable declarations.
            // These "variables" are then involved in dataflow analysis, leading to potential errors.
            // Names marked as special could not be used as identifiers,
            // hence such placeholder variables were created not by the user and
            // should be excluded from the analysis.
            if (node.fir.name.isSpecial) return

            val graph = node.owner.nearestNonInPlaceGraph()
            if (node.fir.initializer != null) {
                data.unreadWrites.add(node.fir)
            }
            data.variableScopes[node.fir.symbol] = graph
            data.variablesWithoutReads[node.fir.symbol] = node.fir
        }

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            val propertySymbol = node.fir.calleeReference?.toResolvedPropertySymbol() ?: return
            if (propertySymbol.name.isSpecial) return

            if (propertySymbol in data.localProperties) {
                data.unreadWrites.add(node.fir)
            }
        }
    }

    /**
     * Back-propagate all variable assignments as [PropertyAccessType.Captured]. This information
     * is then used when calculating visible assignments for each node.
     */
    private class FindCapturedWrites(
        private val properties: Set<FirPropertySymbol>,
    ) : PathAwareControlFlowGraphVisitor<PropertyAccessType, VariableWriteData>(CfgTraverseDirection.Backward) {

        override fun mergeInfo(
            a: ControlFlowInfo<PropertyAccessType, VariableWriteData>,
            b: ControlFlowInfo<PropertyAccessType, VariableWriteData>,
            node: CFGNode<*>,
        ): ControlFlowInfo<PropertyAccessType, VariableWriteData> {
            return a.merge(b, VariableWriteData::plus)
        }

        override fun visitEdge(
            from: CFGNode<*>, to: CFGNode<*>, metadata: Edge,
            data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
        ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
            if (metadata.label == CapturedByValue) return persistentMapOf() // Ignore CapturedByValue edges.

            return when (from) {
                // Do not propagate captured writes beyond the property declaration.
                is VariableDeclarationNode -> super.visitEdge(from, to, metadata, data.remove(from.fir.symbol))

                else -> super.visitEdge(from, to, metadata, data)
            }
        }

        override fun visitVariableDeclarationNode(
            node: VariableDeclarationNode,
            data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
        ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
            return when {
                node.fir.name.isSpecial || node.fir.initializer == null -> data
                else -> data.add(PropertyAccessType.Captured, node.fir.symbol, node)
            }
        }

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
        ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
            val symbol = node.fir.calleeReference?.toResolvedPropertySymbol()?.takeIf { it in properties } ?: return data
            if (symbol.name.isSpecial) return data
            return data.add(PropertyAccessType.Captured, symbol, node)
        }
    }

    /**
     * Forward-propagates the most recent assignments as [PropertyAccessType.InPlace] for each node
     * and combines this with [PropertyAccessType.Captured] assignments from [FindCapturedWrites].
     */
    private class FindVisibleWrites(
        private val futureWrites: Map<CFGNode<*>, PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>>,
        private val properties: Set<FirPropertySymbol>,
    ) : PathAwareControlFlowGraphVisitor<PropertyAccessType, VariableWriteData>() {

        override fun mergeInfo(
            a: ControlFlowInfo<PropertyAccessType, VariableWriteData>,
            b: ControlFlowInfo<PropertyAccessType, VariableWriteData>,
            node: CFGNode<*>,
        ): ControlFlowInfo<PropertyAccessType, VariableWriteData> {
            return a.merge(b, VariableWriteData::plus)
        }

        override fun visitEdge(
            from: CFGNode<*>, to: CFGNode<*>, metadata: Edge,
            data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
        ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
            if (metadata.label == CapturedByValue) return persistentMapOf() // Ignore CapturedByValue edges.
            var result = super.visitEdge(from, to, metadata, data)

            val fromGraph = from.owner.nearestNonInPlaceGraph()
            val toGraph = to.owner.nearestNonInPlaceGraph()
            when {
                // Inherit parent-graph captured writes to properties as visible writes.
                // Subgraphs should inherit their own captured writes if they are not in-place,
                // as they could be concurrently executed with themselves, and any write could be read.
                fromGraph != toGraph -> {
                    val capturedWrites = futureWrites[from]
                    if (capturedWrites != null) {
                        val parentInfo = super.visitEdge(from, to, metadata, capturedWrites)
                        result = result.merge(parentInfo) { a, b -> mergeInfo(a, b, to) }
                    }
                }

                // Inherit non-in-place subgraph captured writes to properties as visible writes.
                from is CFGNodeWithSubgraphs<*> -> {
                    for (subGraph in from.subGraphs) {
                        if (fromGraph != subGraph.nearestNonInPlaceGraph()) {
                            val node = subGraph.enterNode
                            val nodeData = super.visitEdge(from, node, metadata, futureWrites[node] ?: continue)
                            result = result.merge(nodeData) { a, b -> mergeInfo(a, b, to) }
                        }
                    }
                }
            }

            return result
        }

        override fun visitVariableDeclarationNode(
            node: VariableDeclarationNode,
            data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
        ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> =
            if (node.fir.name.isSpecial) data
            else data
                .remove(node.fir.symbol) // Remove all captured writes of property as well.
                .overwrite(node.fir.symbol, if (node.fir.initializer != null) persistentSetOf(node) else persistentSetOf())

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
        ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
            val symbol = node.fir.calleeReference?.toResolvedPropertySymbol()?.takeIf { it in properties } ?: return data
            if (symbol.name.isSpecial) return data
            return data.overwrite(symbol, persistentSetOf(node))
        }
    }

    private class RemoveVisibleWrites(private val data: VariableAssignmentData) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {
            visitAnnotations(node)
        }

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
            visitAnnotations(node)
            visitQualifiedAccess(node, node.fir)
        }

        override fun visitFunctionCallExitNode(node: FunctionCallExitNode) {
            visitAnnotations(node)
            // TODO, KT-64094: receiver of implicit invoke calls does not generate a QualifiedAccessNode.
            ((node.fir as? FirImplicitInvokeCall)?.explicitReceiver?.unwrapSmartcastExpression() as? FirQualifiedAccessExpression)
                ?.let { visitQualifiedAccess(node, it) }
        }

        private fun visitAnnotations(node: CFGNode<*>) {
            // Annotations don't create CFG nodes. Note that annotations can only refer to `const val`s, so any read
            // of a local inside an annotation is inherently incorrect. Still, use them to suppress the warnings.
            (node.fir as? FirAnnotationContainer)?.annotations?.forEach { call ->
                call.accept(object : FirVisitorVoid() {
                    override fun visitElement(element: FirElement) {
                        if (element is FirQualifiedAccessExpression) {
                            visitQualifiedAccess(node, element)
                        }
                        element.acceptChildren(this)
                    }
                })
            }
        }

        private fun visitQualifiedAccess(node: CFGNode<*>, fir: FirQualifiedAccessExpression) {
            val symbol = fir.calleeReference.toResolvedPropertySymbol() ?: return
            if (symbol.name.isSpecial) return
            data.variablesWithoutReads.remove(symbol)
            for (visibleByLabel in data.writesByNode[node]?.values.orEmpty()) {
                for (visible in visibleByLabel.values) {
                    visible[symbol]?.forEach { data.unreadWrites.remove(it.fir) }
                }
            }
        }
    }
}

private class VariableAssignmentData(val localProperties: Set<FirPropertySymbol>) {
    var writesByNode: Map<CFGNode<*>, PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>> = emptyMap()
    val unreadWrites: MutableSet<FirStatement /* FirProperty | FirVariableAssignment */> = mutableSetOf()
    val variableScopes: MutableMap<FirPropertySymbol, ControlFlowGraph> = hashMapOf()
    val variablesWithoutReads: MutableMap<FirPropertySymbol, FirProperty> = mutableMapOf()
}

private enum class PropertyAccessType {
    InPlace,
    Captured,
}

@JvmInline
private value class VariableWriteData(val value: PersistentMap<FirPropertySymbol, PersistentSet<CFGNode<*>>>) {
    constructor(symbol: FirPropertySymbol, node: CFGNode<*>) : this(persistentMapOf(symbol to persistentSetOf(node)))
    constructor(symbol: FirPropertySymbol, data: PersistentSet<CFGNode<*>>) : this(persistentMapOf(symbol to data))

    operator fun get(symbol: FirPropertySymbol): PersistentSet<CFGNode<*>>? = value[symbol]

    operator fun plus(other: VariableWriteData): VariableWriteData {
        return VariableWriteData(value.merge(other.value, PersistentSet<CFGNode<*>>::addAll))
    }

    fun add(symbol: FirPropertySymbol, node: CFGNode<*>): VariableWriteData {
        val nodes = value[symbol] ?: persistentSetOf()
        return VariableWriteData(value.put(symbol, nodes.add(node)))
    }

    fun remove(symbol: FirPropertySymbol): VariableWriteData? {
        val newValue = value.remove(symbol)
        return when {
            newValue.isEmpty() -> null
            else -> VariableWriteData(newValue)
        }
    }

    operator fun iterator(): Iterator<Map.Entry<FirPropertySymbol, PersistentSet<CFGNode<*>>>> {
        return value.iterator()
    }
}

private fun PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>.add(
    type: PropertyAccessType,
    symbol: FirPropertySymbol,
    node: CFGNode<*>,
): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
    return transformValues { oldData ->
        oldData.put(type, oldData[type]?.add(symbol, node) ?: VariableWriteData(symbol, node))
    }
}

private fun PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>.remove(
    symbol: FirPropertySymbol,
): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
    return transformValues { oldData ->
        oldData.mutate {
            for ((type, writes) in oldData) {
                when (val value = writes.remove(symbol)) {
                    null -> it.remove(type)
                    else -> it[type] = value
                }
            }
        }
    }
}

private fun PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>.overwrite(
    symbol: FirPropertySymbol, nodes: PersistentSet<CFGNode<*>>,
): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
    return transformValues {
        val data = when (val data = it[PropertyAccessType.InPlace]) {
            null -> VariableWriteData(symbol, nodes)
            else -> VariableWriteData(data.value.put(symbol, nodes))
        }
        it.put(PropertyAccessType.InPlace, data)
    }
}

@Suppress("unused") // Used in debugger
private fun ControlFlowGraph.render(data: Map<CFGNode<*>, PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>>): String {
    return render(options = ControlFlowGraphRenderOptions(data = { node ->
        buildString {
            for ((edge, nodeData) in data[node].orEmpty()) {
                for ((type, variableWriteData) in nodeData) {
                    appendLine("${edge.label} - $type")
                    for ((variable, writes) in variableWriteData) {
                        appendLine("    $variable")
                        for (node in writes) {
                            appendLine("        ${node.fir.render()}")
                        }
                    }
                }
            }
        }
    }))
}

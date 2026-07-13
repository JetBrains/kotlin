/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.dsl

import org.jetbrains.kotlin.backend.common.dependencies.AccessibleIndex
import org.jetbrains.kotlin.backend.common.dependencies.AnonymousInitializerIndex
import org.jetbrains.kotlin.backend.common.dependencies.BeginInstanceInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.BeginStaticInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.DependencyEdge
import org.jetbrains.kotlin.backend.common.dependencies.DependencyGraph
import org.jetbrains.kotlin.backend.common.dependencies.DependencyGraph.Companion.condenseCycles
import org.jetbrains.kotlin.backend.common.dependencies.DependencyNode
import org.jetbrains.kotlin.backend.common.dependencies.DependencyNodeIndex
import org.jetbrains.kotlin.backend.common.dependencies.EndInstanceInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.EndStaticInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.FunctionIndex
import org.jetbrains.kotlin.backend.common.dependencies.IsCalledBy
import org.jetbrains.kotlin.backend.common.dependencies.IsReferencedBy
import org.jetbrains.kotlin.backend.common.dependencies.MayHappenBefore
import org.jetbrains.kotlin.backend.common.dependencies.MustHappenBefore
import org.jetbrains.kotlin.backend.common.dependencies.PropertyIndex
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.asFileEntity
import org.jetbrains.kotlin.backend.common.dependencies.util.beginInitializationIndex
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import java.util.Deque
import java.util.LinkedList
import kotlin.sequences.forEach

@DslMarker
annotation class DependencyGraphBuilderDsl

inline fun DependencyGraph.buildGraph(worklist: Deque<DependencyNodeIndex>, init: DependencyGraphBuilder.() -> Unit) {
    DependencyGraphBuilder(this, worklist).apply(init)
}

class DependencyGraphBuilderContext(val dependencyGraph: DependencyGraph, val worklist: Deque<DependencyNodeIndex>) {
    internal val dirtyNodes: MutableSet<DependencyNodeIndex> = mutableSetOf()

    fun reset() {
        dirtyNodes.clear()
    }
}

@DependencyGraphBuilderDsl
sealed class DependencyNodeBuilder(internal val context: DependencyGraphBuilderContext) {

    val dependencyGraph: DependencyGraph get() = context.dependencyGraph

    val worklist: Deque<DependencyNodeIndex> get() = context.worklist

    fun DependencyNodeIndex.buildNode() {
        context.dependencyGraph.getOrCreate(this) {
            context.dirtyNodes += this
            context.worklist.add(this)
        }
    }

    fun <D : IrDeclaration> IrBindableSymbol<*, D>.postponeFileEntity() {
        val enclosingEntity = owner.fileOrNull?.asFileEntity() ?: return
        worklist.add(enclosingEntity.beginInitializationIndex)
    }

    fun IrClassSymbol.postponeInitSubgraph() {
        worklist.add(beginInitializationIndex)
        postponeFileEntity()
    }

    fun IrClass.postponeInitSubgraph() = symbol.postponeInitSubgraph()

    private fun addEdge(edge: DependencyEdge): Boolean {
        val addedFrom = context.dependencyGraph[edge.from]?.insertOutgoingEdge(edge) ?: false
        val addedTo = context.dependencyGraph[edge.to]?.insertIncomingEdge(edge) ?: false
        if (addedFrom) context.dirtyNodes += edge.from
        if (addedTo) context.dirtyNodes += edge.to
        return addedFrom || addedTo
    }

    context(at: IrExpression?)
    infix fun DependencyNodeIndex.calls(from: FunctionIndex<*>): Boolean = let { index ->
        when {
            from != index && from in context.dependencyGraph && index in context.dependencyGraph ->
                addEdge(IsCalledBy(from, index, at))
            else -> false
        }
    }

    context(at: IrExpression?)
    infix fun DependencyNodeIndex.references(from: AccessibleIndex): Boolean = let { index ->
        when {
            from != index && from in context.dependencyGraph && index in context.dependencyGraph ->
                addEdge(IsReferencedBy(from, index, at))
            else -> false
        }
    }

    infix fun DependencyNodeIndex.mustHappenBefore(to: DependencyNodeIndex): Boolean = let { index ->
        when {
            index != to && index in context.dependencyGraph && to in context.dependencyGraph -> {
                addEdge(MustHappenBefore(index, to))
            }
            else -> false
        }
    }

    infix fun DependencyNodeIndex.mayHappenBefore(to: DependencyNodeIndex): Boolean = let { index ->
        when {
            index != to && index in context.dependencyGraph && to in context.dependencyGraph -> {
                addEdge(MayHappenBefore(index, to))
            }
            else -> false
        }
    }
}

@DependencyGraphBuilderDsl
class DependencyGraphBuilder(
    dependencyGraph: DependencyGraph,
    worklist: Deque<DependencyNodeIndex>
) : DependencyNodeBuilder(DependencyGraphBuilderContext(dependencyGraph, worklist)) {

    inline fun <D : IrSymbolOwner, E : EnclosingEntity<D>> E.buildClinitSubgraph(crossinline init: StaticInitializationSubgraphBuilder<D, E>.() -> Unit = {}) {
        // Build the subgraph using the initializer
        StaticInitializationSubgraphBuilder(this@DependencyGraphBuilder, this).apply {
            beginInitializationIndex.buildSubgraphNode()
            init()
            endInitializationIndex.buildSubgraphNode()
        }
    }

    inline fun IrClassSymbol.buildInitSubgraph(crossinline init: InstanceInitializationSubgraphBuilder.() -> Unit = {}) {
        InstanceInitializationSubgraphBuilder(this@DependencyGraphBuilder, this).apply {
            BeginInstanceInitializationIndex(this@buildInitSubgraph).buildSubgraphNode()
            init()
            EndInstanceInitializationIndex(this@buildInitSubgraph).buildSubgraphNode()
        }
    }

    /**
     * Condenses the graph by removing multi-node strongly connected components and replacing them with composite nodes
     */
    fun condenseGraph() {
        if (context.dirtyNodes.isEmpty()) return
        val queue = LinkedList<DependencyNode>()

        context(context.dependencyGraph) {
            // Collect all forward reachable nodes from the marked dirty nodes
            context.dirtyNodes.asSequence().mapNotNull(context.dependencyGraph::get).forEach(queue::add)
            val forwardReachable = linkedSetOf<DependencyNode>()
            while (queue.isNotEmpty()) {
                val first = queue.pop()
                if (forwardReachable.add(first)) {
                    first.happenAfter.forEach(queue::add)
                }
            }

            // Collect all backwards reachable nodes from the marked dirty nodes
            context.dirtyNodes.asSequence().mapNotNull(context.dependencyGraph::get).forEach(queue::add)
            val backwardReachable = linkedSetOf<DependencyNode>()
            while (queue.isNotEmpty()) {
                val first = queue.pop()
                if (backwardReachable.add(first)) {
                    first.happenBefore.forEach(queue::add)
                }
            }

            // Consider only nodes that are reachable from both directions (dirtyNodes are subsumed by this),
            // and condense any cycles that are formed
            forwardReachable.intersect(backwardReachable).condenseCycles()
        }

        // Clear the dirty nodes
        context.reset()
    }
}

@DependencyGraphBuilderDsl
sealed class DependencySubgraphBuilder(
    delegate: DependencyNodeBuilder,
    startWith: DependencyNodeIndex
) : DependencyNodeBuilder(delegate.context) {

    private val outerSubgraphBuilder: DependencySubgraphBuilder? = delegate as? DependencySubgraphBuilder
    var lastConstructedNode: DependencyNodeIndex = startWith
        private set(value) {
            field mustHappenBefore value
            field = value
            outerSubgraphBuilder?.lastConstructedNode = value
        }

    protected fun buildSubgraphNode(node: DependencyNodeIndex) {
        node.buildNode()
        lastConstructedNode = node
    }
}

@DependencyGraphBuilderDsl
class StaticInitializationSubgraphBuilder<D : IrSymbolOwner, E : EnclosingEntity<D>>(
    delegate: DependencyNodeBuilder,
    val enclosingEntity: E
) : DependencySubgraphBuilder(delegate, enclosingEntity.beginInitializationIndex) {

    fun BeginStaticInitializationIndex<D>.buildSubgraphNode() {
        require(enclosingEntity == this@StaticInitializationSubgraphBuilder.enclosingEntity) {
            "The begin static initialization node $this must be constructed in the context of its enclosing entity ($enclosingEntity) and not ${this@StaticInitializationSubgraphBuilder.enclosingEntity}!"
        }
        buildSubgraphNode(this)
    }

    fun EndStaticInitializationIndex<D>.buildSubgraphNode() {
        require(enclosingEntity == this@StaticInitializationSubgraphBuilder.enclosingEntity) {
            "The begin static initialization node $this must be constructed in the context of its enclosing entity ($enclosingEntity) and not ${this@StaticInitializationSubgraphBuilder.enclosingEntity}!"
        }
        buildSubgraphNode(this)
    }

    fun PropertyIndex.buildSubgraphNode() {
        require(enclosingEntity == this@StaticInitializationSubgraphBuilder.enclosingEntity) {
            "The static property node $this must be constructed in the context of its enclosing entity ($enclosingEntity) and not ${this@StaticInitializationSubgraphBuilder.enclosingEntity}!"
        }
        buildSubgraphNode(this)
    }

    fun AnonymousInitializerIndex.buildSubgraphNode() {
        require(enclosingEntity == this@StaticInitializationSubgraphBuilder.enclosingEntity) {
            "The static initializer node $this must be constructed in the context of its enclosing entity ($enclosingEntity) and not ${this@StaticInitializationSubgraphBuilder.enclosingEntity}!"
        }
        buildSubgraphNode(this)
    }

    inline fun <D : IrSymbolOwner, E : EnclosingEntity<D>> E.buildNestedSubgraph(crossinline init: StaticInitializationSubgraphBuilder<D, E>.() -> Unit = {}) {
        require(enclosingEntity == parentEnclosingEntity) {
            "The given enclosing entity ($this) must directly nested under the outer entity ($enclosingEntity)!"
        }
        // Construct the subgraph's begin node already here, as it needs to be connected to the lastConstructedNode
        StaticInitializationSubgraphBuilder(this@StaticInitializationSubgraphBuilder, this).apply {
            beginInitializationIndex.buildSubgraphNode()
            init()
            endInitializationIndex.buildSubgraphNode()
        }
    }
}

@DependencyGraphBuilderDsl
class InstanceInitializationSubgraphBuilder(
    delegate: DependencyNodeBuilder,
    val symbol: IrClassSymbol
) : DependencySubgraphBuilder(delegate, symbol.beginInitializationIndex) {

    fun BeginInstanceInitializationIndex.buildSubgraphNode() {
        require(symbol == this@InstanceInitializationSubgraphBuilder.symbol) {
            "The begin instance initialization node $this must be constructed in the context of its class ($symbol) and not ${this@InstanceInitializationSubgraphBuilder.symbol}!"
        }
        buildSubgraphNode(this)
    }

    fun EndInstanceInitializationIndex.buildSubgraphNode() {
        require(symbol == this@InstanceInitializationSubgraphBuilder.symbol) {
            "The begin instance initialization node $this must be constructed in the context of its class ($symbol) and not ${this@InstanceInitializationSubgraphBuilder.symbol}!"
        }
        buildSubgraphNode(this)
    }

    fun PropertyIndex.buildSubgraphNode() {
        val containingClass = symbol.owner.parentClassOrNull?.symbol
        require(containingClass == this@InstanceInitializationSubgraphBuilder.symbol) {
            "The instance property node $this must be constructed in the context of its class ($containingClass) and not ${this@InstanceInitializationSubgraphBuilder.symbol}!"
        }
        buildSubgraphNode(this)
    }

    fun AnonymousInitializerIndex.buildSubgraphNode() {
        val containingClass = symbol.owner.parentClassOrNull?.symbol
        require(containingClass == this@InstanceInitializationSubgraphBuilder.symbol) {
            "The instance initializer node $this must be constructed in the context of its class ($containingClass) and not ${this@InstanceInitializationSubgraphBuilder.symbol}!"
        }
        buildSubgraphNode(this)
    }
}

typealias ClassSubgraphBuilder = StaticInitializationSubgraphBuilder<IrClass, EnclosingEntity.Class>
typealias ObjectSubgraphBuilder = StaticInitializationSubgraphBuilder<IrClass, EnclosingEntity.Object>
typealias EnumEntrySubgraphBuilder = StaticInitializationSubgraphBuilder<IrEnumEntry, EnclosingEntity.EnumEntry>
typealias FileSubgraphBuilder = StaticInitializationSubgraphBuilder<IrFile, EnclosingEntity.File>

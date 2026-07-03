/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies.dsl

import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.dependencies.AccessibleIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.AnonymousInitializerIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.BeginInstanceInitializationIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.BeginStaticInitializationIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyEdge
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraph
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraph.Companion.condenseCycles
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyNode
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyNodeIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asFileEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EndInstanceInitializationIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.EndStaticInitializationIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.FunctionIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.IsCalledBy
import org.jetbrains.kotlin.fir.resolve.dependencies.IsReferencedBy
import org.jetbrains.kotlin.fir.resolve.dependencies.MayHappenBefore
import org.jetbrains.kotlin.fir.resolve.dependencies.MustHappenBefore
import org.jetbrains.kotlin.fir.resolve.dependencies.PropertyIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.beginInitializationIndex
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
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

    fun FirCallableSymbol<*>.postponeFileEntity() {
        val enclosingEntity = dependencyGraph.session.firProvider.getContainingFile(this)?.symbol?.asFileEntity() ?: return
        worklist.add(enclosingEntity.beginInitializationIndex)
    }

    fun FirClassSymbol<*>.postponeFileEntity() {
        val file = dependencyGraph.session.firProvider.getContainingFile(this)?.symbol ?: return
        val enclosingEntity = file.asFileEntity()
        worklist.add(enclosingEntity.beginInitializationIndex)
    }

    fun FirClassSymbol<*>.postponeInitSubgraph() {
        worklist.add(beginInitializationIndex)
        postponeFileEntity()
    }

    private fun addEdge(edge: DependencyEdge): Boolean {
        val addedFrom = context.dependencyGraph[edge.from]?.insertOutgoingEdge(edge) ?: false
        val addedTo = context.dependencyGraph[edge.to]?.insertIncomingEdge(edge) ?: false
        if (addedFrom) context.dirtyNodes += edge.from
        if (addedTo) context.dirtyNodes += edge.to
        return addedFrom || addedTo
    }

    context(at: FirExpression)
    infix fun DependencyNodeIndex.calls(from: FunctionIndex<*>): Boolean = let { index ->
        when {
            from != index && from in context.dependencyGraph && index in context.dependencyGraph ->
                addEdge(IsCalledBy(from, index, at.source))
            else -> false
        }
    }

    context(at: FirExpression)
    infix fun DependencyNodeIndex.references(from: AccessibleIndex): Boolean = let { index ->
        when {
            from != index && from in context.dependencyGraph && index in context.dependencyGraph ->
                addEdge(IsReferencedBy(from, index, at.source))
            else -> false
        }
    }

    infix fun DependencyNodeIndex.mustHappenBefore(to: DependencyNodeIndex): Boolean = let { index ->
        when {
            index != to && index in context.dependencyGraph && to in context.dependencyGraph -> {
                val actualFrom = context.dependencyGraph[index]?.index ?: return false
                val actualTo = context.dependencyGraph[to]?.index ?: return false
                addEdge(MustHappenBefore(actualFrom, actualTo))
            }
            else -> false
        }
    }

    infix fun DependencyNodeIndex.mayHappenBefore(to: DependencyNodeIndex): Boolean = let { index ->
        when {
            index != to && index in context.dependencyGraph && to in context.dependencyGraph -> {
                val actualFrom = context.dependencyGraph[index]?.index ?: return false
                val actualTo = context.dependencyGraph[to]?.index ?: return false
                addEdge(MayHappenBefore(actualFrom, actualTo))
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

    inline fun <D : FirDeclaration, E : EnclosingEntity<D>> E.buildClinitSubgraph(crossinline init: StaticInitializationSubgraphBuilder<D, E>.() -> Unit = {}) {
        // Build the subgraph using the initializer
        beginInitializationIndex.buildNode()
        StaticInitializationSubgraphBuilder(this@DependencyGraphBuilder, this).apply {
            init()
            endInitializationIndex.buildSubgraphNode()
        }
    }

    inline fun <C : FirClass> FirClassSymbol<C>.buildInitSubgraph(crossinline init: InstanceInitializationSubgraphBuilder<C>.() -> Unit = {}) {
        BeginInstanceInitializationIndex(this).buildNode()
        InstanceInitializationSubgraphBuilder(this@DependencyGraphBuilder, this).apply {
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
class StaticInitializationSubgraphBuilder<D : FirDeclaration, E : EnclosingEntity<D>>(
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

    inline fun <D : FirDeclaration, E : EnclosingEntity<D>> E.buildNestedSubgraph(crossinline init: StaticInitializationSubgraphBuilder<D, E>.() -> Unit = {}) {
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
class InstanceInitializationSubgraphBuilder<C : FirClass>(
    delegate: DependencyNodeBuilder,
    val classSymbol: FirClassSymbol<C>
) : DependencySubgraphBuilder(delegate, classSymbol.beginInitializationIndex) {

    fun BeginInstanceInitializationIndex<C>.buildSubgraphNode() {
        require(classSymbol == this@InstanceInitializationSubgraphBuilder.classSymbol) {
            "The begin instance initialization node $this must be constructed in the context of its class ($classSymbol) and not ${this@InstanceInitializationSubgraphBuilder.classSymbol}!"
        }
        buildSubgraphNode(this)
    }

    fun EndInstanceInitializationIndex<C>.buildSubgraphNode() {
        require(classSymbol == this@InstanceInitializationSubgraphBuilder.classSymbol) {
            "The begin instance initialization node $this must be constructed in the context of its class ($classSymbol) and not ${this@InstanceInitializationSubgraphBuilder.classSymbol}!"
        }
        buildSubgraphNode(this)
    }

    fun PropertyIndex.buildSubgraphNode() {
        val propertyClassSymbol = symbol.containingClassLookupTag()?.toClassSymbol(symbol.moduleData.session)
        require(propertyClassSymbol == this@InstanceInitializationSubgraphBuilder.classSymbol) {
            "The instance property node $this must be constructed in the context of its class ($propertyClassSymbol) and not ${this@InstanceInitializationSubgraphBuilder.classSymbol}!"
        }
        buildSubgraphNode(this)
    }

    fun AnonymousInitializerIndex.buildSubgraphNode() {
        val initializerClassSymbol = symbol.containingDeclarationSymbol
        require(initializerClassSymbol == this@InstanceInitializationSubgraphBuilder.classSymbol) {
            "The instance initializer node $this must be constructed in the context of its class ($initializerClassSymbol) and not ${this@InstanceInitializationSubgraphBuilder.classSymbol}!"
        }
        buildSubgraphNode(this)
    }
}

typealias ClassSubgraphBuilder = StaticInitializationSubgraphBuilder<FirRegularClass, EnclosingEntity.Class>
typealias ObjectSubgraphBuilder = StaticInitializationSubgraphBuilder<FirRegularClass, EnclosingEntity.Object>
typealias EnumEntrySubgraphBuilder = StaticInitializationSubgraphBuilder<FirEnumEntry, EnclosingEntity.EnumEntry>
typealias FileSubgraphBuilder = StaticInitializationSubgraphBuilder<FirFile, EnclosingEntity.File>

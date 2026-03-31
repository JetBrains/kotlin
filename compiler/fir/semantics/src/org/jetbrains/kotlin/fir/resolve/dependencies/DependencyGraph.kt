/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.processAllClassifiers
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirBooleanOperatorExpression
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirDesugaredAssignmentValueReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirIncrementDecrementExpression
import org.jetbrains.kotlin.fir.expressions.FirIntegerLiteralOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.FirMultiDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSamConversionExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.expressions.FirSuperReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.ExplicitlyPassedSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraph.DependencyNode.Companion.accesses
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraph.DependencyNode.Companion.happensBefore
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraph.DependencyNode.Companion.mayHappenBefore
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraph.DependencyNode.Companion.stronglyConnectedComponents
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraph.DependencyNode.CondensedNode.Companion.condense
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.Dependency
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.EnclosingEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.EnclosingEntity.Companion.asClassEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.EnclosingEntity.Companion.asEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.EnclosingEntity.Companion.asEnumEntryEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.EnclosingEntity.Companion.asFileEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.EnclosingEntity.Companion.asInstancedPropertyEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.EnclosingEntity.Companion.asObjectEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.EnclosingEntity.Companion.outermostEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.NodeIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.NodeIndex.Companion.beginIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.NodeIndex.Companion.endIndex
import org.jetbrains.kotlin.fir.resolve.dfa.Stack
import org.jetbrains.kotlin.fir.resolve.dfa.isNotEmpty
import org.jetbrains.kotlin.fir.resolve.dfa.stackOf
import org.jetbrains.kotlin.fir.resolve.dfa.topOrNull
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.isEnum
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.mapToIndex
import java.util.LinkedList
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.contains
import kotlin.collections.set
import kotlin.sequences.forEach

data class DependencyGraph(
    private val nodes: MutableMap<NodeIndex<*>, DependencyNode<*>> = linkedMapOf(),
    private val entities: MutableMap<EnclosingEntity<*>, MutableSet<DependencyNode<*>>> = linkedMapOf()
) : FirSessionComponent, Set<DependencyGraph.DependencyNode<*>> {

    private val badNodes = mutableSetOf<NodeIndex<*>>()

    val enclosingEntities: Set<EnclosingEntity<*>> get() = entities.keys

    override val size: Int get() = nodes.size

    override fun isEmpty(): Boolean = nodes.isEmpty()

    override fun contains(element: DependencyNode<*>): Boolean = element.index in this

    override fun iterator(): Iterator<DependencyNode<*>> = nodes.values.iterator()

    override fun containsAll(elements: Collection<DependencyNode<*>>): Boolean = elements.all { it in this }

    operator fun get(index: NodeIndex<*>): DependencyNode<*>? = nodes[index]

    operator fun get(enclosingEntity: EnclosingEntity<*>): Sequence<DependencyNode<*>> =
        entities[enclosingEntity]?.asSequence() ?: emptySequence()

    operator fun contains(index: NodeIndex<*>): Boolean = index in nodes

    operator fun contains(enclosingEntity: EnclosingEntity<*>): Boolean = enclosingEntity in enclosingEntities

    fun badAccessesFor(index: NodeIndex<*>, visited: MutableSet<NodeIndex<*>> = mutableSetOf()): Set<FirExpression> {
        require(index in badNodes)
        return this[index]?.let { node ->
            when (node) {
                is DependencyNode.CompositeNode -> mutableSetOf<FirExpression>().apply {
                    val canShortcircuit = when (index) {
                        is NodeIndex.DeclarationIndex -> index.symbol is FirPropertySymbol && index.symbol.hasInitializer || index.symbol is FirAnonymousInitializerSymbol
                        is NodeIndex.BeginSubgraphIndex -> {
                            if (index.enclosingEntity is EnclosingEntity.InstancedProperty) {
                                index.enclosingEntity.symbol.hasInitializer
                            } else {
                                index.enclosingEntity is EnclosingEntity.EnumEntry || index.enclosingEntity is EnclosingEntity.Object
                            }
                        }
                        else -> false
                    }

                    // KT-20238: We must check that we are in a cycle with the property's enclosing entity, and such that
                    // the enclosing entity is a companion object nested in an interface
                    fun isEnclosedInCompanionOfInterface(index: NodeIndex<*>) = when (index) {
                        is NodeIndex.DeclarationIndex ->
                            index.containingEntity is EnclosingEntity.Object
                                    && index.containingEntity in node
                                    && index.containingEntity.outerEnclosingEntity is EnclosingEntity.Class
                                    && index.containingEntity.outerEnclosingEntity.symbol.isInterface
                        is NodeIndex.BeginSubgraphIndex ->
                            index.enclosingEntity is EnclosingEntity.InstancedProperty
                                    && index.enclosingEntity.outerEnclosingEntity is EnclosingEntity.Object
                                    && index.enclosingEntity.outerEnclosingEntity in node
                                    && index.enclosingEntity.outerEnclosingEntity.outerEnclosingEntity is EnclosingEntity.Class
                                    && index.enclosingEntity.outerEnclosingEntity.outerEnclosingEntity.symbol.isInterface
                        else -> false
                    }
                    node.accessesFor(index).forEach { (from, exprs) ->
                        if (canShortcircuit && from in node || from !in node && (isEnclosedInCompanionOfInterface(from) || isBad(from, visited))) {
                            addAll(exprs)
                        }
                    }
                }
                is DependencyNode.SingletonNode -> mutableSetOf<FirExpression>().apply {
                    node.accesses.forEach { (from, exprs) ->
                        if (isBad(from, visited)) {
                            addAll(exprs)
                        }
                    }
                }
            }
        } ?: emptySet()
    }

    fun isBad(index: NodeIndex<*>, visited: MutableSet<NodeIndex<*>> = mutableSetOf()): Boolean {
        if (index !in this) return false
        if (index in badNodes) return true
        if (index in visited) {
            badNodes += index
            return true
        }
        // Visit the index and check its accesses
        visited.add(index)
        return this[index]?.let { node ->
            when (node) {
                // If it belongs to a composite node, ...
                is DependencyNode.CompositeNode -> {
                    val accesses = node.accessesFor(index)
                    // Base case: if any information flowing to the node is from a node in its own strong component (time loop)
                    val canShortcircuit = when (index) {
                        is NodeIndex.DeclarationIndex -> index.symbol is FirPropertySymbol && index.symbol.hasInitializer || index.symbol is FirAnonymousInitializerSymbol
                        is NodeIndex.BeginSubgraphIndex -> {
                            if (index.enclosingEntity is EnclosingEntity.InstancedProperty) {
                                index.enclosingEntity.symbol.hasInitializer
                            } else {
                                index.enclosingEntity is EnclosingEntity.EnumEntry || index.enclosingEntity is EnclosingEntity.Object
                            }
                        }
                        else -> false
                    }
                    if (canShortcircuit && accesses.any { (from, _) -> from.apply(::println) in node }) {
                        println("found one")
                        badNodes += index
                        return@let true
                    }
                    // KT-20238: We must check that we are in a cycle with the property's enclosing entity, and such that
                    // the enclosing entity is a companion object nested in an interface
                    fun isEnclosedInCompanionOfInterface(index: NodeIndex<*>) = when (index) {
                        is NodeIndex.DeclarationIndex ->
                            index.containingEntity is EnclosingEntity.Object
                                    && index.containingEntity in node
                                    && index.containingEntity.outerEnclosingEntity is EnclosingEntity.Class
                                    && index.containingEntity.outerEnclosingEntity.symbol.isInterface
                        is NodeIndex.BeginSubgraphIndex ->
                            index.enclosingEntity is EnclosingEntity.InstancedProperty
                                    && index.enclosingEntity.outerEnclosingEntity is EnclosingEntity.Object
                                    && index.enclosingEntity.outerEnclosingEntity in node
                                    && index.enclosingEntity.outerEnclosingEntity.outerEnclosingEntity is EnclosingEntity.Class
                                    && index.enclosingEntity.outerEnclosingEntity.outerEnclosingEntity.symbol.isInterface
                        else -> false
                    }
                    // Inductive step: if any information flowing to the node is from a node that is bad (elsewhere)
                    if (canShortcircuit
                        && accesses.any { (from, _) ->
                            from !in node && (isEnclosedInCompanionOfInterface(from) || isBad(from, visited))
                        }) {
                        badNodes += index
                        return@let true
                    }
                    false
                }
                // If it belongs to a singleton node, ...
                is DependencyNode.SingletonNode<*> -> {
                    // Inductive step: if any information flowing to the node is from a node that is bad (elsewhere)
                    if (node.accesses.any { (from, _) -> isBad(from, visited) }) {
                        badNodes += index
                        return@let true
                    }
                    false
                }
            }
        } ?: false
    }

    fun deadlockingEntities(enclosingEntity: EnclosingEntity<*>): Sequence<EnclosingEntity<*>> =
        this[enclosingEntity].filterIsInstance<DependencyNode.CondensedNode>()
            .flatMap { it.enclosingEntities }
            .filter { it == enclosingEntity }
            .distinct()

    override fun toString(): String {
        val builder = StringBuilder()
        Printer(builder).apply {
            println("digraph DependencyGraph {")
            pushIndent()
            println("graph [overlap = true, fontsize = 10]")
            val nodes = mapToIndex()
            nodes.forEach { (node, index) ->
                println(
                    "n$index [shape=${
                        when (node) {
                            is DependencyNode.PropertyNode,
                            is DependencyNode.FunctionNode<*>,
                            is DependencyNode.QualifierNode,
                            is DependencyNode.EnumEntryNode,
                            is DependencyNode.CompositeNode,
                            is DependencyNode.InstancedPropertyNode
                                -> "circle"
                            is DependencyNode.InitializerBlockNode,
                            is DependencyNode.ClinitNode,
                            is DependencyNode.TopLevelNode,
                            is DependencyNode.EndInitializationNode<*>
                                -> "box"
                        }
                    }, label=\"${node.renderAsString()}\"]"
                )
            }
            println()
            nodes.forEach { (node, index) ->
                node.outgoing.forEach { dependency ->
                    this@DependencyGraph[dependency.to]?.let(nodes::get)?.let { childIndex ->
                        println(
                            "n$index -> n$childIndex [color=${
                                when (dependency) {
                                    is Dependency.Access -> "blue"
                                    is Dependency.HappensBefore -> "red"
                                    is Dependency.MayHappenBefore -> "green"
                                }
                            }]"
                        )
                    }
                }
            }
            popIndent()
            println("}")
        }
        return builder.toString()
    }

    sealed class DependencyNode<out D : FirDeclaration> {

        abstract val index: NodeIndex<D>

        abstract val isComposite: Boolean

        abstract val incoming: Sequence<Dependency>

        abstract val outgoing: Sequence<Dependency>

        abstract val happenBefore: Sequence<NodeIndex<*>>

        abstract val possiblyHappenBefore: Sequence<NodeIndex<*>>

        abstract val happenAfter: Sequence<NodeIndex<*>>

        abstract val possiblyHappenAfter: Sequence<NodeIndex<*>>

        abstract fun accessesTo(from: NodeIndex<*>): Set<FirExpression>

        protected abstract fun addIncomingAccess(access: Dependency.Access, at: FirExpression): Boolean

        protected abstract fun addIncomingAccess(access: Dependency.Access, at: Set<FirExpression>): Boolean

        protected abstract fun addOutgoingAccess(access: Dependency.Access): Boolean

        protected abstract fun addIncomingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean

        protected abstract fun removeIncomingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean

        protected abstract fun addOutgoingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean

        protected abstract fun removeOutgoingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean

        fun renderAsString(): String = index.toString()

        sealed class SingletonNode<out D : FirDeclaration> : DependencyNode<D>() {
            abstract val enclosingEntity: EnclosingEntity<*>
            override val isComposite: Boolean = false

            private val _incomingInfoFlow = linkedMapOf<Dependency.Access, MutableSet<FirExpression>>()
            private val _incomingTimeFlow = linkedSetOf<Dependency.TimeDependency>()
            override val incoming: Sequence<Dependency>
                get() = sequence {
                    yieldAll(_incomingInfoFlow.keys)
                    yieldAll(_incomingTimeFlow)
                }

            private val _outgoingInfoFlow = linkedSetOf<Dependency.Access>()
            private val _outgoingTimeFlow = linkedSetOf<Dependency.TimeDependency>()
            override val outgoing: Sequence<Dependency>
                get() = sequence {
                    yieldAll(_outgoingInfoFlow)
                    yieldAll(_outgoingTimeFlow)
                }

            override val happenBefore: Sequence<NodeIndex<*>>
                get() = _incomingTimeFlow.asSequence()
                    .filter(Dependency.TimeDependency::alwaysHappens)
                    .map(Dependency::from)

            override val possiblyHappenBefore: Sequence<NodeIndex<*>>
                get() = _incomingTimeFlow.asSequence()
                    .filter(Dependency.TimeDependency::possiblyHappens)
                    .map(Dependency::from)

            override val happenAfter: Sequence<NodeIndex<*>>
                get() = _outgoingTimeFlow.asSequence()
                    .filter(Dependency.TimeDependency::alwaysHappens)
                    .map(Dependency::to)

            override val possiblyHappenAfter: Sequence<NodeIndex<*>>
                get() = _outgoingTimeFlow.asSequence()
                    .filter(Dependency.TimeDependency::possiblyHappens)
                    .map(Dependency::to)

            override fun accessesTo(from: NodeIndex<*>): Set<FirExpression> =
                _incomingInfoFlow[Dependency.Access(from, index)] ?: emptySet()

            val accesses: Sequence<Pair<NodeIndex<*>, Set<FirExpression>>>
                get() = _incomingInfoFlow.asSequence().map { (access, accesses) -> access.from to accesses }

            override fun addIncomingAccess(access: Dependency.Access, at: FirExpression): Boolean {
                if (access.to != index) return false
                return _incomingInfoFlow.getOrPut(access) { mutableSetOf() }.add(at)
            }

            override fun addIncomingAccess(access: Dependency.Access, at: Set<FirExpression>): Boolean {
                if (access.to != index) return false
                return _incomingInfoFlow.getOrPut(access) { mutableSetOf() }.addAll(at)
            }

            override fun addOutgoingAccess(access: Dependency.Access): Boolean {
                if (access.from != index) return false
                return _outgoingInfoFlow.add(access)
            }

            override fun addIncomingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean {
                if (timeDependency.to != index) return false
                return _incomingTimeFlow.add(timeDependency)
            }

            override fun removeIncomingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean {
                if (timeDependency.to != index) return false
                return _incomingTimeFlow.remove(timeDependency)
            }

            override fun addOutgoingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean {
                if (timeDependency.from != index) return false
                return _outgoingTimeFlow.add(timeDependency)
            }

            override fun removeOutgoingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean {
                if (timeDependency.from != index) return false
                return _outgoingTimeFlow.remove(timeDependency)
            }
        }

        sealed class CompositeNode : DependencyNode<Nothing>(), Set<NodeIndex<*>> {
            abstract override val index: NodeIndex.CompositeIndex
            override val isComposite: Boolean = true

            private val _incomingAccesses = linkedMapOf<Dependency.Access, MutableSet<FirExpression>>()
            private val _incomingInfoFlow = linkedMapOf<NodeIndex<*>, MutableSet<Dependency.Access>>()
            private val _incomingTimeFlow = linkedSetOf<Dependency.TimeDependency>()
            override val incoming: Sequence<Dependency>
                get() = sequence {
                    for ((_, accesses) in _incomingInfoFlow) {
                        yieldAll(accesses)
                    }
                    yieldAll(_incomingTimeFlow)
                }

            private val _outgoingInfoFlow = linkedMapOf<NodeIndex<*>, MutableSet<Dependency.Access>>()
            private val _outgoingTimeFlow = linkedSetOf<Dependency.TimeDependency>()
            override val outgoing: Sequence<Dependency>
                get() = sequence {
                    for ((_, accesses) in _outgoingInfoFlow) {
                        yieldAll(accesses)
                    }
                    yieldAll(_outgoingTimeFlow)
                }

            override val happenBefore: Sequence<NodeIndex<*>>
                get() = _incomingTimeFlow.asSequence()
                    .filter(Dependency.TimeDependency::alwaysHappens)
                    .map(Dependency::from)

            override val possiblyHappenBefore: Sequence<NodeIndex<*>>
                get() = _incomingTimeFlow.asSequence()
                    .filter(Dependency.TimeDependency::possiblyHappens)
                    .map(Dependency::from)

            override val happenAfter: Sequence<NodeIndex<*>>
                get() = _outgoingTimeFlow.asSequence()
                    .filter(Dependency.TimeDependency::alwaysHappens)
                    .map(Dependency::to)

            override val possiblyHappenAfter: Sequence<NodeIndex<*>>
                get() = _outgoingTimeFlow.asSequence()
                    .filter(Dependency.TimeDependency::possiblyHappens)
                    .map(Dependency::to)

            override fun accessesTo(from: NodeIndex<*>): Set<FirExpression> =
                fold(linkedSetOf()) { acc, index ->
                    _incomingAccesses[Dependency.Access(from, index)]?.apply(acc::addAll)
                    acc
                }

            fun accessesFor(index: NodeIndex<*>): Sequence<Pair<NodeIndex<*>, Set<FirExpression>>> =
                _incomingInfoFlow[index]?.asSequence()?.mapNotNull { access ->
                    _incomingAccesses[access]?.let { exprs -> access.from to exprs }
                } ?: emptySequence()

            abstract val enclosingEntities: Set<EnclosingEntity<*>>
            abstract fun get(enclosingEntity: EnclosingEntity<*>): Sequence<NodeIndex<*>>
            operator fun contains(enclosingEntity: EnclosingEntity<*>): Boolean = enclosingEntity in enclosingEntities

            override fun addIncomingAccess(access: Dependency.Access, at: FirExpression): Boolean {
                if (access.to !in this@CompositeNode) return false
                val addedFlow = _incomingInfoFlow.getOrPut(access.to) { linkedSetOf() }.add(access)
                val addedAt = _incomingAccesses.getOrPut(access) { linkedSetOf() }.add(at)
                return addedFlow || addedAt
            }

            override fun addIncomingAccess(access: Dependency.Access, at: Set<FirExpression>): Boolean {
                if (access.to !in this@CompositeNode) return false
                val addedFlow = _incomingInfoFlow.getOrPut(access.to) { linkedSetOf() }.add(access)
                val addedAt = _incomingAccesses.getOrPut(access) { linkedSetOf() }.addAll(at)
                return addedFlow || addedAt
            }

            override fun addOutgoingAccess(access: Dependency.Access): Boolean {
                if (access.from !in this@CompositeNode) return false
                return _outgoingInfoFlow.getOrPut(access.from) { linkedSetOf() }.add(access)
            }

            override fun addIncomingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean {
                if (timeDependency.to != index) return false
                return _incomingTimeFlow.add(timeDependency)
            }

            override fun removeIncomingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean {
                if (timeDependency.to != index) return false
                return _incomingTimeFlow.remove(timeDependency)
            }

            override fun addOutgoingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean {
                if (timeDependency.from != index) return false
                return _outgoingTimeFlow.add(timeDependency)
            }

            override fun removeOutgoingTimeDependency(timeDependency: Dependency.TimeDependency): Boolean {
                if (timeDependency.from != index) return false
                return _outgoingTimeFlow.remove(timeDependency)
            }
        }

        sealed class BeginInitializationNode<D : FirDeclaration> : SingletonNode<D>() {
            abstract override val enclosingEntity: EnclosingEntity<D>
            override val index: NodeIndex.BeginSubgraphIndex<D> by lazy { enclosingEntity.beginIndex() }
        }

        data class EndInitializationNode<D : FirDeclaration>(override val enclosingEntity: EnclosingEntity<D>) : SingletonNode<D>() {
            override val index: NodeIndex.EndSubgraphIndex<D> = enclosingEntity.endIndex()
        }

        /**
         * Represents access to a static property (i.e., to a top-level property, an object property, or an enum entry property)
         */
        data class PropertyNode(
            override val index: NodeIndex.DeclarationIndex<FirProperty>
        ) : SingletonNode<FirProperty>() {
            override val enclosingEntity: EnclosingEntity<*> get() = index.containingEntity
        }

        data class InitializerBlockNode(
            override val index: NodeIndex.DeclarationIndex<FirAnonymousInitializer>
        ) : SingletonNode<FirAnonymousInitializer>() {
            override val enclosingEntity: EnclosingEntity<*> get() = index.containingEntity
        }

        data class FunctionNode<D : FirFunction>(
            override val index: NodeIndex.DeclarationIndex<D>,
        ) : SingletonNode<D>() {
            override val enclosingEntity: EnclosingEntity<*> get() = index.containingEntity
        }

        data class QualifierNode(
            override val enclosingEntity: EnclosingEntity.Object
        ) : BeginInitializationNode<FirRegularClass>()

        data class TopLevelNode(
            override val enclosingEntity: EnclosingEntity.File
        ) : BeginInitializationNode<FirFile>()

        data class ClinitNode(
            override val enclosingEntity: EnclosingEntity.Class
        ) : BeginInitializationNode<FirRegularClass>()

        data class EnumEntryNode(
            override val enclosingEntity: EnclosingEntity.EnumEntry
        ) : BeginInitializationNode<FirEnumEntry>()

        data class InstancedPropertyNode(
            override val enclosingEntity: EnclosingEntity.InstancedProperty
        ) : BeginInitializationNode<FirProperty>()

        data class CondensedNode(
            private val indices: Set<NodeIndex<*>>,
            private val entities: MutableMap<EnclosingEntity<*>, MutableSet<NodeIndex<*>>>
        ) : CompositeNode(), Set<NodeIndex<*>> by indices {
            override val index: NodeIndex.CompositeIndex = NodeIndex.CompositeIndex(indices)
            override val enclosingEntities: Set<EnclosingEntity<*>> get() = entities.keys
            override operator fun get(enclosingEntity: EnclosingEntity<*>): Sequence<NodeIndex<*>> =
                entities[enclosingEntity]?.asSequence() ?: emptySequence()

            override operator fun contains(element: NodeIndex<*>): Boolean = element in indices

            companion object {
                context(graph: DependencyGraph)
                fun Set<DependencyNode<*>>.condense(): CondensedNode = CondensedNode(
                    indices = this.mapTo(linkedSetOf(), DependencyNode<*>::index),
                    entities = mutableMapOf<EnclosingEntity<*>, MutableSet<NodeIndex<*>>>().also { entities ->
                        this.forEach { node ->
                            when (node) {
                                is SingletonNode<*> -> entities.getOrPut(node.enclosingEntity) { linkedSetOf() }.add(node.index)
                                is CondensedNode -> entities.putAll(node.entities)
                            }
                        }
                    }
                ).apply {
                    // Add the node to the graph
                    graph.nodes[index] = this
                    // For each node in the set that was condensed, ...
                    this@condense.forEach { node ->
                        // For each incoming dependency, ...
                        node.incoming.forEach { dependency ->
                            when (dependency) {
                                // Add all incoming access edges from each node (regardless if their targets are in the set of not)
                                is Dependency.Access -> addIncomingAccess(dependency, node.accessesTo(dependency.from))
                                // Merge all incoming time dependencies into the new condensed node and update their targets
                                is Dependency.HappensBefore -> {
                                    // Skip edges which connect nodes in the set
                                    if (dependency.from in this) return@forEach
                                    val newDependency = Dependency.HappensBefore(dependency.from, index)
                                    addIncomingTimeDependency(newDependency)
                                    graph[dependency.from]?.let { from ->
                                        from.removeOutgoingTimeDependency(dependency)
                                        from.addOutgoingTimeDependency(newDependency)
                                    }
                                }
                                is Dependency.MayHappenBefore -> {
                                    // Skip edges which connect nodes in the set
                                    if (dependency.from in this) return@forEach
                                    val newDependency = Dependency.MayHappenBefore(dependency.from, index)
                                    addIncomingTimeDependency(newDependency)
                                    graph[dependency.from]?.let { from ->
                                        from.removeOutgoingTimeDependency(dependency)
                                        from.addOutgoingTimeDependency(newDependency)
                                    }
                                }
                            }
                        }
                        // For each outgoing dependency, ...
                        node.outgoing.forEach { dependency ->
                            when (dependency) {
                                // Add all outgoing access edges from each node (regardless if their targets are in the set of not)
                                is Dependency.Access -> addOutgoingAccess(dependency)
                                // Merge all incoming time dependencies into the new condensed node and update their targets
                                is Dependency.HappensBefore -> {
                                    // Skip edges which connect nodes in the set
                                    if (dependency.to in this) return@forEach
                                    val newDependency = Dependency.HappensBefore(index, dependency.to)
                                    addOutgoingTimeDependency(newDependency)
                                    graph[dependency.from]?.let { from ->
                                        from.removeIncomingTimeDependency(dependency)
                                        from.addIncomingTimeDependency(newDependency)
                                    }
                                }
                                is Dependency.MayHappenBefore -> {
                                    // Skip edges which connect nodes in the set
                                    if (dependency.to in this) return@forEach
                                    val newDependency = Dependency.MayHappenBefore(index, dependency.to)
                                    addOutgoingTimeDependency(newDependency)
                                    graph[dependency.from]?.let { from ->
                                        from.removeIncomingTimeDependency(dependency)
                                        from.addIncomingTimeDependency(newDependency)
                                    }
                                }
                            }
                        }
                        // Update the graph's indices
                        when (node) {
                            is SingletonNode<*> -> {
                                // For singleton nodes, we keep the mapping of their node indices to this condensed node,
                                // as we require their presence in the graph for further analysis of their accesses
                                graph.nodes[node.index] = this
                                graph.entities[node.enclosingEntity]?.let { nodes ->
                                    nodes.remove(node)
                                    nodes.add(this)
                                }
                            }
                            is CompositeNode -> {
                                // For composite nodes, they are only preserved through time dependencies, so once the node
                                // is detached, it has no accesses by itself and can be safely removed from the graph
                                graph.nodes.remove(node.index)
                                node.enclosingEntities.asSequence()
                                    .mapNotNull(graph.entities::get)
                                    .forEach { nodes ->
                                        nodes.remove(node)
                                        nodes.add(this)
                                    }
                            }
                        }
                    }
                }
            }
        }

        companion object {

            context(graph: DependencyGraph)
            infix fun NodeIndex<*>?.accesses(access: Pair<NodeIndex<*>, FirExpression>): Boolean {
                val (other, at) = access
                // Disallow self-loops
                if (this != null && this != other && this in graph && other in graph) {
                    val dependency = Dependency.Access(other, this)
                    val addedLeft = graph[this]?.addIncomingAccess(dependency, at) ?: false
                    val addedRight = graph[other]?.addOutgoingAccess(dependency) ?: false
                    return addedLeft || addedRight
                }
                return false
            }

            context(graph: DependencyGraph)
            infix fun NodeIndex<*>?.happensBefore(other: NodeIndex<*>): Boolean {
                // Disallow self-loops
                if (this != null && this != other && this in graph && other in graph) {
                    val actualLeftIndex = graph[this]?.index ?: return false
                    val actualRightIndex = graph[other]?.index ?: return false
                    val dependency = Dependency.HappensBefore(actualLeftIndex, actualRightIndex)
                    val addedLeft = graph[this]?.addOutgoingTimeDependency(dependency) ?: false
                    val addedRight = graph[other]?.addIncomingTimeDependency(dependency) ?: false
                    return addedLeft || addedRight
                }
                return false
            }

            context(graph: DependencyGraph)
            infix fun NodeIndex<*>?.mayHappenBefore(other: NodeIndex<*>): Boolean {
                // Disallow self-loops
                if (this != null && this != other && this in graph && other in graph) {
                    val actualLeftIndex = graph[this]?.index ?: return false
                    val actualRightIndex = graph[other]?.index ?: return false
                    val dependency = Dependency.MayHappenBefore(actualLeftIndex, actualRightIndex)
                    val addedLeft = graph[this]?.addOutgoingTimeDependency(dependency) ?: false
                    val addedRight = graph[other]?.addIncomingTimeDependency(dependency) ?: false
                    return addedLeft || addedRight
                }
                return false
            }

            context(graph: DependencyGraph)
            inline fun DependencyNode<*>.possiblyHappenAncestors(
                visited: MutableSet<DependencyNode<*>> = mutableSetOf(),
                traversalOrder: TraversalOrder = TraversalOrder.PreOrder,
                crossinline predicate: (DependencyNode<*>) -> Boolean = { true }
            ): Sequence<DependencyNode<*>> =
                traversalOrder.traverse(
                    start = this@possiblyHappenAncestors,
                    visited = visited,
                    predicate = predicate,
                    neighbours = { it.possiblyHappenBefore.mapNotNull(graph::get) }
                )

            context(graph: DependencyGraph)
            inline fun DependencyNode<*>.possiblyHappenDescendants(
                visited: MutableSet<DependencyNode<*>> = mutableSetOf(),
                traversalOrder: TraversalOrder = TraversalOrder.PreOrder,
                crossinline predicate: (DependencyNode<*>) -> Boolean = { true }
            ): Sequence<DependencyNode<*>> =
                traversalOrder.traverse(
                    start = this@possiblyHappenDescendants,
                    visited = visited,
                    predicate = predicate,
                    neighbours = { it.possiblyHappenAfter.mapNotNull(graph::get) }
                )

            context(graph: DependencyGraph)
            fun Set<DependencyNode<*>>.stronglyConnectedComponents(): List<Set<DependencyNode<*>>> {
                val visited = mutableSetOf<DependencyNode<*>>()
                val sorted = stackOf<DependencyNode<*>>()
                this@stronglyConnectedComponents.forEach { node ->
                    node.possiblyHappenDescendants(visited, TraversalOrder.PostOrder) { it in this }
                        .forEach(sorted::push)
                }
                visited.clear()

                val result = LinkedList<Set<DependencyNode<*>>>()
                while (sorted.isNotEmpty) {
                    val current = sorted.pop()
                    if (current !in visited) {
                        val component = mutableSetOf<DependencyNode<*>>()
                        current.possiblyHappenAncestors(visited, TraversalOrder.PostOrder) { it in this }
                            .forEach { component += it }
                        result += component
                    }
                }

                return result
            }
        }
    }

    class Builder(
        override val session: FirSession,
        override val scopeSession: ScopeSession,
        val graph: DependencyGraph = session.dependencyGraph,
    ) : FirVisitorVoid(), SessionAndScopeSessionHolder {

        private data class SubgraphScope(
            val visitingEntity: EnclosingEntity<*>,
            var lastConstructedNode: NodeIndex<*>,
            val firstUses: MutableMap<NodeIndex<*>, NodeIndex<*>> = mutableMapOf(),
        )

        private val scopes: Stack<SubgraphScope> = stackOf()
        private val visiting: Stack<FirElement> = stackOf()
        private val initializedEntities: MutableSet<EnclosingEntity<*>> = mutableSetOf()
        private val dirtyNodes: MutableSet<NodeIndex<*>> = mutableSetOf()

        private inline fun <E : FirElement> E.visit(
            crossinline block: E.() -> Unit
        ) {
            visiting.push(this)
            try {
                block(this)
            } finally {
                visiting.pop()
            }
        }

        private val visitingEntity: EnclosingEntity<*>?
            get() = scopes.topOrNull()?.visitingEntity

        private var lastConstructedNode: NodeIndex<*>?
            get() = scopes.topOrNull()?.lastConstructedNode
            set(value) = value?.let { scopes.topOrNull()?.lastConstructedNode = it } ?: Unit

        private val firstUses: MutableMap<NodeIndex<*>, NodeIndex<*>>?
            get() = scopes.topOrNull()?.firstUses

        private fun NodeIndex<*>?.accesses(other: NodeIndex<*>, at: FirExpression): Boolean =
            context(graph) {
                (this accesses (other to at)).ifTrue {
                    this?.let { dirtyNodes.add(it) }
                    dirtyNodes.add(other)
                    true
                } ?: false
            }

        private fun NodeIndex<*>?.happensBefore(other: NodeIndex<*>): Boolean =
            context(graph) {
                (this happensBefore other).ifTrue {
                    this?.let { dirtyNodes.add(it) }
                    dirtyNodes.add(other)
                    true
                } ?: false
            }

        private fun NodeIndex<*>?.mayHappenBefore(other: NodeIndex<*>): Boolean =
            context(graph) {
                (this mayHappenBefore other).ifTrue {
                    this?.let { dirtyNodes.add(it) }
                    dirtyNodes.add(other)
                    true
                } ?: false
            }

        private fun <E : EnclosingEntity<*>> E.markFirstUse(whenSimilar: Boolean = false): E = apply {
            visitingEntity?.let { visitingEntity ->
                // Disallow marking of first uses of an entity due to an access from its outer entities
                val outermostEntity = outermostEntity
                if (visitingEntity.outermostEntity != outermostEntity || whenSimilar) {
                    lastConstructedNode?.let { lastConstructedNode ->
                        firstUses?.let { firstUses ->
                            val index = outermostEntity.endIndex()
                            if (index !in firstUses) {
                                buildEndInitializationNode(
                                    enclosingEntity = outermostEntity,
                                    initialize = false
                                )
                                index.mayHappenBefore(lastConstructedNode)
                                firstUses[index] = lastConstructedNode
                            }
                        }
                    }
                }
            }
        }

        private fun DependencyNode<*>.markFirstUse(): DependencyNode<*> = apply {
            lastConstructedNode?.let { lastConstructedNode ->
                scopes.topOrNull()?.firstUses?.let { firstUses ->
                    if (index !in firstUses) {
                        index.happensBefore(lastConstructedNode)
                        firstUses[index] = lastConstructedNode
                    }
                }
            }
        }

        @OptIn(SymbolInternals::class)
        private val FirClassSymbol<*>.inheritancePropagatedDeclarations: Sequence<FirDeclaration>
            get() = session.propagatedDeclarationsStorage.propagatedDeclarations.getValue(this, this@Builder)
                .asSequence().map { it.fir }

        private val FirClass.inheritancePropagatedDeclarations: Sequence<FirDeclaration>
            get() = symbol.inheritancePropagatedDeclarations

        private fun FirResolvedQualifier.toEnclosingEntity(): EnclosingEntity<FirRegularClass>? = symbol?.let { symbol ->
            symbol.fullyExpandedClass(symbol.moduleData.session)?.let {
                if (resolvedToCompanionObject && canBeValue) {
                    it.resolvedCompanionObjectSymbol?.asObjectEntity(it.asClassEntity())
                } else if (it.classKind.isObject && canBeValue) {
                    it.asObjectEntity()
                } else {
                    it.asClassEntity()
                }
            }
        }

        private fun FirPropertyAccessExpression.toEnclosingEntity(): EnclosingEntity<*>? =
            calleeReference.toResolvedEnumEntrySymbol()?.asEnumEntryEntity()
                ?: calleeReference.toResolvedPropertySymbol()?.let { propertySymbol ->
                    if (propertySymbol.resolvedStatus.visibility != Visibilities.Public) return@let null
                    if (propertySymbol.resolvedReturnType.isPrimitiveOrNullablePrimitive) return@let null
                    val enclosingEntity = (dispatchReceiver ?: extensionReceiver)?.let { receiver ->
                        when (receiver) {
                            is FirSuperReceiverExpression, is FirThisReceiverExpression -> visitingEntity
                            is FirResolvedQualifier -> receiver.toEnclosingEntity()
                            is FirPropertyAccessExpression -> receiver.toEnclosingEntity()
                            else -> null
                        }
                    } ?: propertySymbol.containingFileSymbol?.asFileEntity()
                    enclosingEntity?.let { propertySymbol.asInstancedPropertyEntity(it) }
                }

        private fun FirFunctionCall.toNodeIndex(): NodeIndex.DeclarationIndex<FirFunction>? =
            calleeReference.toResolvedFunctionSymbol()?.let { functionSymbol ->
                if (functionSymbol.resolvedStatus.visibility != Visibilities.Public) return@let null
                val enclosingEntity = (dispatchReceiver ?: extensionReceiver)?.let { receiver ->
                    when (receiver) {
                        is FirSuperReceiverExpression, is FirThisReceiverExpression -> visitingEntity
                        is FirResolvedQualifier -> receiver.toEnclosingEntity()
                        is FirPropertyAccessExpression -> receiver.toEnclosingEntity()
                        else -> null
                    }
                } ?: functionSymbol.containingFileSymbol?.asFileEntity()
                enclosingEntity?.let { NodeIndex.DeclarationIndex(it, functionSymbol) }
            }

        private inline fun <D : FirDeclaration, T : DependencyNode<D>> buildNode(
            index: NodeIndex<D>,
            enclosingEntity: EnclosingEntity<*>,
            crossinline new: () -> T,
            crossinline connect: NodeIndex<*>?.(NodeIndex<*>) -> Boolean,
            noinline init: ((DependencyNode<*>) -> Unit)? = null
        ): DependencyNode<*> =
            (graph[index] ?: new().apply {
                // Store the node under its index and its entity
                graph.nodes[index] = this
                graph.entities.getOrPut(enclosingEntity) { linkedSetOf() }.add(this)
                // Mark the new node dirty
                dirtyNodes.add(index)
            }).apply {
                // Connect the node to the top of the stack
                lastConstructedNode.connect(index)
                // Initialize the node iff it is being accessed/constructed whilst visiting the subgraph of its enclosing entity
                // NOTE: this happens AT MOST once during graph construction, i.e., when visiting the enclosing entity's declaration
                init?.invoke(this)
            }

        private inline fun <D : FirDeclaration, E : EnclosingEntity<D>, T : DependencyNode.BeginInitializationNode<D>> buildBeginInitializationNode(
            enclosingEntity: E,
            crossinline new: (E) -> T,
            crossinline connect: NodeIndex<*>?.(NodeIndex<*>) -> Boolean = { _, _ -> false },
            crossinline init: (DependencyNode<*>) -> Unit,
        ): DependencyNode<*> = enclosingEntity.beginIndex().let { index ->
            buildNode(
                index = index,
                enclosingEntity = enclosingEntity,
                new = { new(enclosingEntity) },
                connect = connect,
                init = {
                    // Set this node to be the last constructed, as the constructor or initializer dependencies need to be connected to this node
                    val prevNode = lastConstructedNode
                    lastConstructedNode = index
                    // Initialize the node
                    init(it)
                    lastConstructedNode = prevNode
                    // Push the new subgraph scope of the enclosing entity onto the stack
                    val scope = SubgraphScope(
                        visitingEntity = enclosingEntity,
                        lastConstructedNode = index,
                        firstUses = scopes.topOrNull()?.takeIf { scope ->
                            scope.visitingEntity == enclosingEntity.outerEnclosingEntity
                        }?.firstUses ?: mutableMapOf()
                    )
                    scopes.push(scope)
                }
            )
        }

        private inline fun <D : FirDeclaration, E : EnclosingEntity<D>, T : DependencyNode.BeginInitializationNode<D>> buildBeginInitializationNode(
            enclosingEntity: E,
            crossinline new: (E) -> T,
            crossinline connect: NodeIndex<*>?.(NodeIndex<*>) -> Boolean = { _, _ -> false },
        ): DependencyNode<*> = buildNode(enclosingEntity.beginIndex(), enclosingEntity, { new(enclosingEntity) }, connect)

        private inline fun <D : FirDeclaration, E : EnclosingEntity<D>> buildEndInitializationNode(
            enclosingEntity: E,
            initialize: Boolean = true,
            crossinline connect: NodeIndex<*>?.(NodeIndex<*>) -> Boolean = { _, _ -> false },
        ): DependencyNode<*> =
            // We never request to build the end node when recursively visiting its entity's declaration since it is a dummy node.
            // It is fully initialized only at the end of the entity's subgraph
            when (initialize && visitingEntity == enclosingEntity) {
                true -> buildNode(
                    index = enclosingEntity.endIndex(),
                    enclosingEntity = enclosingEntity,
                    new = { DependencyNode.EndInitializationNode(enclosingEntity) },
                    connect = connect,
                    init = {
                        // Pop the subgraph scope of the enclosing entity from the stack, as it is now fully constructed
                        scopes.pop()
                    }
                )
                false -> buildNode(
                    index = enclosingEntity.endIndex(),
                    enclosingEntity = enclosingEntity,
                    new = { DependencyNode.EndInitializationNode(enclosingEntity) },
                    connect = connect
                )
            }

        private inline fun <D : FirDeclaration, T : DependencyNode<D>> buildDeclarationNode(
            index: NodeIndex.DeclarationIndex<D>,
            crossinline new: (NodeIndex.DeclarationIndex<D>) -> T,
            crossinline connect: NodeIndex<*>?.(NodeIndex<*>) -> Boolean = { _, _ -> false },
            crossinline init: (DependencyNode<*>) -> Unit,
        ): DependencyNode<*> = buildNode(
            index = index,
            enclosingEntity = index.containingEntity,
            new = { new(index) },
            connect = connect,
            init = {
                // Set the last constructed node to this one
                lastConstructedNode = index
                // Initialize the node
                init(it)
            })

        private inline fun <D : FirDeclaration, T : DependencyNode<D>> buildDeclarationNode(
            index: NodeIndex.DeclarationIndex<D>,
            crossinline new: (NodeIndex.DeclarationIndex<D>) -> T,
            crossinline connect: NodeIndex<*>?.(NodeIndex<*>) -> Boolean = { _, _ -> false },
        ): DependencyNode<*> = buildNode(index, index.containingEntity, { new(index) }, connect)

        /**
         * Condenses the graph by removing multi-node strongly connected components and replacing them with composite nodes
         */
        private fun condenseGraph() {
            if (dirtyNodes.isEmpty()) return
            val queue = LinkedList<DependencyNode<*>>()

            // Collect all forward reachable nodes from the marked dirty nodes
            dirtyNodes.asSequence().mapNotNull(graph::get).forEach(queue::add)
            val forwardReachable = linkedSetOf<DependencyNode<*>>()
            while (queue.isNotEmpty()) {
                val first = queue.pop()
                if (forwardReachable.add(first)) {
                    first.possiblyHappenAfter.mapNotNull(graph::get).forEach(queue::add)
                }
            }

            // Collect all backwards reachable nodes from the marked dirty nodes
            dirtyNodes.asSequence().mapNotNull(graph::get).forEach(queue::add)
            val backwardReachable = linkedSetOf<DependencyNode<*>>()
            while (queue.isNotEmpty()) {
                val first = queue.pop()
                if (backwardReachable.add(first)) {
                    first.possiblyHappenBefore.mapNotNull(graph::get).forEach(queue::add)
                }
            }

            context(graph) {
                // Consider only nodes that are reachable from both directions (dirtyNodes are subsumed by this)
                // For each strong connected component of size > 1, condense it
                forwardReachable.intersect(backwardReachable)
                    .stronglyConnectedComponents()
                    .forEach { if (it.size > 1) it.condense() }
            }

            // Clear the dirty nodes
            dirtyNodes.clear()
        }

        override fun visitElement(element: FirElement): Unit = Unit

        /**
         * =============================================
         *                  DECLARATIONS
         * =============================================
         */

        @OptIn(DirectDeclarationsAccess::class)
        override fun visitFile(file: FirFile): Unit = file.visit {
            val enclosingEntity = symbol.asFileEntity()
            if (enclosingEntity in initializedEntities) return@visit
            buildBeginInitializationNode(
                enclosingEntity = enclosingEntity,
                new = DependencyNode<FirFile>::TopLevelNode,
                init = {}
            )
            // Keep track of which node has been previously constructed as properties and functions reside in different branches
            var prevNode: NodeIndex<*> = enclosingEntity.beginIndex()
            val functionNodes = mutableSetOf<NodeIndex<*>>()
            file.declarations.forEach { declaration ->
                when (declaration) {
                    is FirProperty -> {
                        declaration.accept(this@Builder)
                        lastConstructedNode?.let { prevNode = it }
                    }
                    is FirFunction -> {
                        declaration.accept(this@Builder)
                        lastConstructedNode?.let { functionNodes += it }
                        lastConstructedNode = prevNode
                    }
                    // it is enclosed inside a file and hence will not be connected to its last constructed node
                    is FirRegularClass if declaration.visibility == Visibilities.Public ->
                        declaration.accept(this@Builder)
                    else -> {}
                }
            }
            buildEndInitializationNode(enclosingEntity) { happensBefore(it) }
            functionNodes.forEach { enclosingEntity.endIndex().mayHappenBefore(it) }
            initializedEntities += enclosingEntity
            // Condense the graph
            condenseGraph()
        }

        @OptIn(ExplicitlyPassedSession::class)
        private fun EnclosingEntity<*>.visitSuperTypes() {
            val classSymbol = when (this) {
                is EnclosingEntity.Class -> symbol
                is EnclosingEntity.Object -> symbol
                is EnclosingEntity.EnumEntry -> symbol.initializerObjectSymbol ?: return
                is EnclosingEntity.InstancedProperty -> symbol.resolvedReturnType.fullyExpandedType().toRegularClassSymbol() ?: return
                else -> return
            }
            classSymbol.resolvedSuperTypes.forEach { superType ->
                superType.fullyExpandedType().toRegularClassSymbol()?.let { classSymbol ->
                    // Skip library supertypes, as they cannot have mutual dependencies with the source types
                    if (classSymbol.moduleData.session.kind == FirSession.Kind.Library) return@let
                    classSymbol.asClassEntity()?.let { enclosingEntity ->
                        // Skip the supertype entity, since we might be in the process of constructing it, and visiting it again will cause
                        // an infinite recursive loop
                        if (enclosingEntity == outerEnclosingEntity && classSymbol.isEnumClass) return@forEach
                        enclosingEntity.symbol.fir.accept(this@Builder)
                        // If the entity is in the graph, it has static declarations, but to connect it to its descendant,
                        // we need to check if it becomes initialized during its static initialization
                        if (enclosingEntity in initializedEntities) {
                            lastConstructedNode?.let {
                                enclosingEntity.endIndex().happensBefore(it)
                            }
                        } else {
                            enclosingEntity.visitSuperTypes()
                        }
                    }
                }
            }
        }

        override fun visitRegularClass(regularClass: FirRegularClass): Unit = regularClass.visit {
            // Case 1: an object with or without inheritance
            if (classKind.isObject) {
                symbol.asObjectEntity(visitingEntity as? EnclosingEntity.Class)?.let { enclosingEntity ->
                    if (enclosingEntity in initializedEntities) return@visit
                    buildBeginInitializationNode(
                        enclosingEntity = enclosingEntity,
                        new = DependencyNode<FirRegularClass>::QualifierNode,
                        connect = {
                            enclosingEntity.outerEnclosingEntity?.let { outer ->
                                // Connect to the last constructed node of the outer entity if we are currently visiting it
                                if (outer == visitingEntity) happensBefore(it) else false
                            } ?: false
                        },
                        init = {
                            enclosingEntity.visitSuperTypes()
                            symbol.primaryConstructorIfAny(session)?.fir?.accept(this@Builder)
                        }
                    )
                    // Keep track of which node has been previously constructed as properties and functions reside in different branches
                    val beginIndex = enclosingEntity.beginIndex()
                    var prevNode: NodeIndex<*> = beginIndex
                    val functionNodes = mutableSetOf<NodeIndex<*>>()
                    inheritancePropagatedDeclarations.forEach { declaration ->
                        when (declaration) {
                            is FirProperty, is FirAnonymousInitializer -> {
                                declaration.accept(this@Builder)
                                lastConstructedNode?.let { prevNode = it }
                            }
                            is FirFunction -> {
                                lastConstructedNode = beginIndex
                                declaration.accept(this@Builder)
                                lastConstructedNode?.let { functionNodes += it }
                                lastConstructedNode = prevNode
                            }
                            else -> {}
                        }
                    }
                    // Build the end node
                    buildEndInitializationNode(enclosingEntity) { happensBefore(it) }
                    // Construct or retrieve the outermost end node to connect its initialization with
                    // this object's function nodes
                    val outermostEntity = enclosingEntity.outermostEntity
                    val outermostIndex = outermostEntity.endIndex()
                    buildEndInitializationNode(
                        enclosingEntity = outermostEntity,
                        initialize = false
                    )
                    functionNodes.forEach { outermostIndex.mayHappenBefore(it) }
                    // Ensure that the last constructed node points to the end node of this subgraph to
                    // maintain the correct happens-before relationship due to initialization order
                    enclosingEntity.outerEnclosingEntity?.let { outer ->
                        if (outer == visitingEntity) lastConstructedNode = enclosingEntity.endIndex()
                    }
                    initializedEntities += enclosingEntity
                    // Condense the graph
                    condenseGraph()
                    // Visit nested classifiers
                    symbol.processAllClassifiers(session) {
                        it.toLookupTag().toClassLikeSymbol()?.resolvedStatus?.let { status ->
                            if (status.visibility == Visibilities.Public) it.fir.accept(this@Builder)
                        }
                    }
                }
            }
            // Case 2: a class with static declarations, i.e., an enum class and/or a class with a companion object
            else if (classKind.isEnumClass || symbol.resolvedCompanionObjectSymbol != null) {
                symbol.asClassEntity()?.let { enclosingEntity ->
                    if (enclosingEntity in initializedEntities) return@visit
                    buildBeginInitializationNode(
                        enclosingEntity = enclosingEntity,
                        new = DependencyNode<FirRegularClass>::ClinitNode,
                        init = {
                            enclosingEntity.visitSuperTypes()
                            symbol.primaryConstructorIfAny(session)?.fir?.accept(this@Builder)
                        }
                    )
                    val nestedClassifiers = mutableSetOf<FirRegularClass>()
                    processAllDeclarations(session) { symbol ->
                        when (val declaration = symbol.fir) {
                            is FirEnumEntry -> declaration.accept(this@Builder)
                            is FirRegularClass if declaration.isCompanion -> declaration.accept(this@Builder)
                            // The classifier is either a public class or an object that is not a companion, either will not be connected
                            // to the last constructed node of this class
                            is FirRegularClass if declaration.visibility == Visibilities.Public -> nestedClassifiers += declaration
                            else -> {}
                        }
                    }
                    buildEndInitializationNode(enclosingEntity) { happensBefore(it) }
                    initializedEntities += enclosingEntity
                    // Condense the graph
                    condenseGraph()
                    // Visit nested classifiers
                    nestedClassifiers.forEach { it.accept(this@Builder) }
                }
            }
        }

        override fun visitEnumEntry(enumEntry: FirEnumEntry): Unit = enumEntry.visit {
            // Recursively visit the anonymous object declaration in its initializer
            acceptChildren(this@Builder)
        }

        override fun visitAnonymousObject(anonymousObject: FirAnonymousObject): Unit =
            anonymousObject.visit {
                symbol.asEnumEntryEntity()?.let { enclosingEntity ->
                    if (enclosingEntity in initializedEntities) return@visit
                    val beginIndex = enclosingEntity.beginIndex()
                    buildBeginInitializationNode(
                        enclosingEntity = enclosingEntity,
                        new = DependencyNode<FirEnumEntry>::EnumEntryNode,
                        connect = {
                            // Connect to the last constructed node of the outer entity if we are currently visiting it
                            if (enclosingEntity.outerEnclosingEntity == visitingEntity) happensBefore(it) else false
                        },
                        init = {
                            enclosingEntity.visitSuperTypes()
                            symbol.primaryConstructorIfAny(session)?.fir?.accept(this@Builder)
                        }
                    )
                    // Keep track of which node has been previously constructed as properties and functions reside in different branches
                    var prevNode: NodeIndex<*> = beginIndex
                    val functionNodes = mutableSetOf<NodeIndex<*>>()
                    inheritancePropagatedDeclarations.forEach { declaration ->
                        when (declaration) {
                            is FirProperty, is FirAnonymousInitializer -> {
                                declaration.accept(this@Builder)
                                lastConstructedNode?.let { prevNode = it }
                            }
                            is FirFunction -> {
                                declaration.accept(this@Builder)
                                lastConstructedNode?.let { functionNodes += it }
                                lastConstructedNode = prevNode
                            }
                            else -> {}
                        }
                    }
                    // Build the end node
                    buildEndInitializationNode(enclosingEntity) { happensBefore(it) }
                    // Construct or retrieve the outermost end node to connect its initialization with
                    // this object's function nodes
                    val outermostEntity = enclosingEntity.outermostEntity
                    val outermostIndex = outermostEntity.endIndex()
                    buildEndInitializationNode(
                        enclosingEntity = outermostEntity,
                        initialize = false
                    )
                    functionNodes.forEach { outermostIndex.mayHappenBefore(it) }
                    if (enclosingEntity.outerEnclosingEntity == visitingEntity) lastConstructedNode = enclosingEntity.endIndex()
                    initializedEntities += enclosingEntity
                    // Visit nested classifiers
                    symbol.processAllClassifiers(session) {
                        it.toLookupTag().toClassLikeSymbol()?.resolvedStatus?.let { status ->
                            if (status.visibility == Visibilities.Public) it.fir.accept(this@Builder)
                        }
                    }
                }
            }

        override fun visitConstructor(constructor: FirConstructor): Unit = constructor.visit {
            delegatedConstructor?.accept(this@Builder)
            body?.accept(this@Builder)
        }

        override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer): Unit =
            anonymousInitializer.visit {
                visitingEntity?.let { visitingEntity ->
                    buildDeclarationNode(
                        index = NodeIndex.DeclarationIndex(visitingEntity, symbol),
                        new = DependencyNode<FirAnonymousInitializer>::InitializerBlockNode,
                        connect = { happensBefore(it) }
                    ) {
                        acceptChildren(this@Builder)
                    }
                }
            }

        override fun visitProperty(property: FirProperty): Unit = property.visit {
            // Visiting a property declaration means that we are interested in initializing its node, i.e., it is not a local property
            // Distinguish between a property with a subgraph, i.e., a one which result type is a class, or a property without a subgraph (primitive type)
            // So far we only care about only vals
            if (!isLocal && isVal) {
                // We should be visiting a subgraph
                visitingEntity?.let { visitingEntity ->
                    returnTypeRef.coneTypeOrNull?.let { type ->
                        // Case 1: property without a subgraph -> primitive type
                        if (type.isPrimitiveOrNullablePrimitive || type.isUnit || type.isNothing) {
                            buildDeclarationNode(
                                index = NodeIndex.DeclarationIndex(visitingEntity, symbol),
                                new = DependencyNode<FirProperty>::PropertyNode,
                                connect = { happensBefore(it) },
                                init = {
                                    initializer?.accept(this@Builder)
                                    getter?.acceptChildren(this@Builder)
                                }
                            )
                        }
                        // Case 2: property with a subgraph -> class type
                        else {
                            type.toRegularClassSymbol()?.let { classSymbol ->
                                val enclosingEntity = symbol.asInstancedPropertyEntity(visitingEntity)
                                if (enclosingEntity in initializedEntities) return@let
                                val beginIndex = enclosingEntity.beginIndex()
                                buildBeginInitializationNode(
                                    enclosingEntity = enclosingEntity,
                                    new = DependencyNode<FirProperty>::InstancedPropertyNode,
                                    connect = { happensBefore(it) },
                                    init = {
                                        // Skip building a graph for static initialization of its library type
                                        if (classSymbol.moduleData.session.kind == FirSession.Kind.Source) {
                                            // Visit the class' declaration as well, as its initialization triggers its clinit
                                            classSymbol.fir.accept(this@Builder)
                                            // The class' clinit (and of its supertypes) happens before the property's initialization
                                            classSymbol.asClassEntity()?.endIndex()?.happensBefore(beginIndex)
                                                ?: enclosingEntity.visitSuperTypes()
                                        }
                                        initializer?.accept(this@Builder)
                                        getter?.acceptChildren(this@Builder)
                                    }
                                )
                                // Keep track of which node has been previously constructed as properties and functions reside in different branches
                                var prevNode: NodeIndex<*> = beginIndex
                                val functionNodes = mutableSetOf<NodeIndex<*>>()
                                classSymbol.inheritancePropagatedDeclarations.forEach { declaration ->
                                    when (declaration) {
                                        is FirProperty, is FirAnonymousInitializer -> {
                                            declaration.accept(this@Builder)
                                            lastConstructedNode?.let { prevNode = it }
                                        }
                                        is FirFunction -> {
                                            declaration.accept(this@Builder)
                                            lastConstructedNode?.let { functionNodes += it }
                                            lastConstructedNode = prevNode
                                        }
                                        else -> {}
                                    }
                                }
                                // Build the end node
                                buildEndInitializationNode(enclosingEntity) { happensBefore(it) }
                                // Construct or retrieve the outermost end node to connect its initialization with
                                // this object's function nodes
                                val outermostEntity = enclosingEntity.outermostEntity
                                val outermostIndex = outermostEntity.endIndex()
                                buildEndInitializationNode(
                                    enclosingEntity = outermostEntity,
                                    initialize = false
                                )
                                functionNodes.forEach { outermostIndex.mayHappenBefore(it) }
                                initializedEntities += enclosingEntity
                                // Ensure that the last constructed node points to the end node of this subgraph to
                                // maintain the correct happens-before relationship due to initialization order
                                if (enclosingEntity.outerEnclosingEntity == visitingEntity) lastConstructedNode = enclosingEntity.endIndex()
                            }
                        }
                    }
                }
            }
        }

        override fun visitFunction(function: FirFunction): Unit = function.visit {
            visitingEntity?.let { visitingEntity ->
                buildDeclarationNode(
                    index = NodeIndex.DeclarationIndex(visitingEntity, symbol),
                    new = DependencyNode<FirFunction>::FunctionNode,
                    init = { acceptChildren(this@Builder) }
                )
            }
        }

        override fun visitNamedFunction(namedFunction: FirNamedFunction): Unit = namedFunction.visit {
            visitingEntity?.let { visitingEntity ->
                buildDeclarationNode(
                    index = NodeIndex.DeclarationIndex(visitingEntity, symbol),
                    new = DependencyNode<FirFunction>::FunctionNode,
                    init = { acceptChildren(this@Builder) }
                )
            }
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction): Unit = anonymousFunction.visit {
            anonymousFunction.acceptChildren(this@Builder)
        }

        /**
         * =============================================
         *                  EXPRESSIONS
         * =============================================
         */

        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression): Unit =
            anonymousFunctionExpression.visit { acceptChildren(this@Builder) }

        override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression): Unit =
            anonymousObjectExpression.visit { acceptChildren(this@Builder) }

        override fun visitBlock(block: FirBlock): Unit =
            block.visit { acceptChildren(this@Builder) }

        override fun visitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression): Unit =
            booleanOperatorExpression.visit { acceptChildren(this@Builder) }

        override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess): Unit =
            callableReferenceAccess.visit { acceptChildren(this@Builder) }

        override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject): Unit =
            checkedSafeCallSubject.visit { originalReceiverRef.value.accept(this@Builder) }

        override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall): Unit =
            checkNotNullCall.visit { acceptChildren(this@Builder) }

        override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression): Unit =
            classReferenceExpression.visit {
                classTypeRef.toRegularClassSymbol(session)?.asEntity()?.let { classEntity ->
                    when (classEntity) {
                        is EnclosingEntity.Class -> buildBeginInitializationNode(
                            enclosingEntity = classEntity.markFirstUse(),
                            new = DependencyNode<FirRegularClass>::ClinitNode,
                            connect = { accesses(classEntity.beginIndex(), classReferenceExpression) }
                        )
                        is EnclosingEntity.Object -> buildBeginInitializationNode(
                            enclosingEntity = classEntity.markFirstUse(),
                            new = DependencyNode<FirRegularClass>::QualifierNode,
                            connect = { accesses(classEntity.beginIndex(), classReferenceExpression) }
                        )
                        else -> return@visit
                    }
                }
            }

        override fun visitCollectionLiteral(collectionLiteral: FirCollectionLiteral): Unit =
            collectionLiteral.visit { acceptChildren(this@Builder) }

        override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression): Unit =
            comparisonExpression.visit { acceptChildren(this@Builder) }

        override fun visitComponentCall(componentCall: FirComponentCall): Unit = componentCall.visit {
            if (dispatchReceiver == null && extensionReceiver != null
                && calleeReference.toResolvedCallableSymbol()?.origin == FirDeclarationOrigin.Library) return@visit
            toNodeIndex()?.let { index ->
                buildDeclarationNode(
                    index = index,
                    new = DependencyNode<FirFunction>::FunctionNode,
                    connect = { accesses(index, componentCall) }
                ).markFirstUse()
            }
        }

        override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall): Unit =
            delegatedConstructorCall.visit {
                calleeReference.toResolvedConstructorSymbol()?.let {
                    if (it.origin == FirDeclarationOrigin.Library) return@visit
                    argumentList.acceptChildren(this@Builder)
                    it.fir.accept(this@Builder)
                }
            }

        override fun visitDesugaredAssignmentValueReferenceExpression(
            desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression
        ): Unit = desugaredAssignmentValueReferenceExpression.visit {
            expressionRef.value.accept(this@Builder)
        }

        override fun visitElvisExpression(elvisExpression: FirElvisExpression): Unit =
            elvisExpression.visit { acceptChildren(this@Builder) }

        override fun visitEnumEntryDeserializedAccessExpression(
            enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression
        ): Unit = enumEntryDeserializedAccessExpression.visit {
            enumClassId.toLookupTag().toSymbol()
                ?.fullyExpandedClass()
                ?.collectEnumEntries()
                ?.find { it.name == enumEntryDeserializedAccessExpression.enumEntryName }
                ?.asEnumEntryEntity()
                ?.let { enumEntry ->
                    buildBeginInitializationNode(
                        enclosingEntity = enumEntry.markFirstUse(),
                        new = DependencyNode<FirEnumEntry>::EnumEntryNode,
                        connect = { accesses(enumEntry.beginIndex(), enumEntryDeserializedAccessExpression) }
                    )
                }
        }

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall): Unit = equalityOperatorCall.visit {
            acceptChildren(this@Builder)
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall): Unit = functionCall.visit {
            argumentList.acceptChildren(this@Builder)
            if (dispatchReceiver == null && extensionReceiver != null
                && calleeReference.toResolvedCallableSymbol()?.origin == FirDeclarationOrigin.Library) return@visit
            toNodeIndex()?.let { index ->
                buildDeclarationNode(
                    index = index,
                    new = DependencyNode<FirFunction>::FunctionNode,
                    connect = { accesses(it, functionCall) }
                ).markFirstUse()
            } ?: dispatchReceiver?.accept(this@Builder)
        }

        override fun visitGetClassCall(getClassCall: FirGetClassCall): Unit = getClassCall.visit {
            argument.acceptChildren(this@Builder)
        }

        override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall): Unit =
            implicitInvokeCall.visit {
                argumentList.acceptChildren(this@Builder)
                if (dispatchReceiver == null && extensionReceiver != null
                    && calleeReference.toResolvedCallableSymbol()?.origin == FirDeclarationOrigin.Library) return@visit
                toNodeIndex()?.let { index ->
                    buildDeclarationNode(
                        index = index,
                        new = DependencyNode<FirFunction>::FunctionNode,
                        connect = { accesses(it, implicitInvokeCall) }
                    ).markFirstUse()
                } ?: dispatchReceiver?.accept(this@Builder)
            }

        override fun visitIncrementDecrementExpression(incrementDecrementExpression: FirIncrementDecrementExpression): Unit =
            incrementDecrementExpression.visit { acceptChildren(this@Builder) }

        override fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall): Unit =
            integerLiteralOperatorCall.visit {
                argumentList.acceptChildren(this@Builder)
                if (dispatchReceiver == null && extensionReceiver != null
                    && calleeReference.toResolvedCallableSymbol()?.origin == FirDeclarationOrigin.Library) return@visit
                toNodeIndex()?.let { index ->
                    buildDeclarationNode(
                        index = index,
                        new = DependencyNode<FirFunction>::FunctionNode,
                        connect = { accesses(index, integerLiteralOperatorCall) }
                    ).markFirstUse()
                } ?: dispatchReceiver?.accept(this@Builder)
            }

        override fun visitLazyBlock(lazyBlock: FirLazyBlock): Unit = lazyBlock.visit { acceptChildren(this@Builder) }

        override fun visitMultiDelegatedConstructorCall(
            multiDelegatedConstructorCall: FirMultiDelegatedConstructorCall
        ) {
            TODO("Not yet implemented")
        }

        override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression): Unit =
            namedArgumentExpression.visit { acceptChildren(this@Builder) }

        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression): Unit =
            propertyAccessExpression.visit {
                // Case 1: accessing an enum entry
                if (resolvedType.isEnum) {
                    calleeReference.toResolvedEnumEntrySymbol()?.asEnumEntryEntity()?.let { enumEntry ->
                        buildBeginInitializationNode(
                            enclosingEntity = enumEntry.markFirstUse(),
                            new = DependencyNode<FirEnumEntry>::EnumEntryNode,
                            connect = { accesses(enumEntry.beginIndex(), propertyAccessExpression) }
                        )
                    }
                }
                // Case 2: accessing an instanced property
                else if (resolvedType.toRegularClassSymbol(session) != null
                    && !resolvedType.isPrimitiveOrNullablePrimitive
                    && !resolvedType.isUnit && !resolvedType.isNothing
                ) {
                    calleeReference.toResolvedPropertySymbol()?.let { propertySymbol ->
                        val outerEnclosingEntity = (dispatchReceiver ?: extensionReceiver)?.let { receiver ->
                            when (receiver) {
                                is FirSuperReceiverExpression, is FirThisReceiverExpression -> visitingEntity
                                is FirResolvedQualifier -> receiver.toEnclosingEntity()
                                is FirPropertyAccessExpression -> receiver.toEnclosingEntity()
                                else -> null
                            }
                        } ?: propertySymbol.containingFileSymbol?.asFileEntity()
                        outerEnclosingEntity?.let { outerEnclosingEntity ->
                            val enclosingEntity = propertySymbol.asInstancedPropertyEntity(outerEnclosingEntity)
                            buildBeginInitializationNode(
                                // Handle accessor-only properties, i.e., create a may-happen-before cycle to the accessed node,
                                // such that the accesses to this accessor-only property can be checked for uninitialized accesses
                                enclosingEntity = enclosingEntity.markFirstUse(whenSimilar = !propertySymbol.hasInitializer),
                                new = DependencyNode<FirProperty>::InstancedPropertyNode,
                                connect = { accesses(it, propertyAccessExpression) }
                            )
                        }
                    }
                }
                // Case 3: accessing a property with a primitive type
                else {
                    calleeReference.toResolvedPropertySymbol()?.let { propertySymbol ->
                        if (propertySymbol.resolvedStatus.visibility != Visibilities.Public) return@let
                        val enclosingEntity = (dispatchReceiver ?: extensionReceiver).let { receiver ->
                            when (receiver) {
                                is FirSuperReceiverExpression, is FirThisReceiverExpression -> visitingEntity
                                is FirResolvedQualifier -> receiver.toEnclosingEntity()
                                is FirPropertyAccessExpression -> receiver.toEnclosingEntity()
                                else -> null
                            }
                        } ?: propertySymbol.containingFileSymbol?.asFileEntity()
                        enclosingEntity?.let { enclosingEntity ->
                            buildDeclarationNode(
                                // Handle accessor-only properties, i.e., create a may-happen-before cycle to the accessed node,
                                // such that the accesses to this accessor-only property can be checked for uninitialized accesses
                                index = NodeIndex.DeclarationIndex(
                                    containingEntity = enclosingEntity.markFirstUse(whenSimilar = !propertySymbol.hasInitializer),
                                    symbol = propertySymbol
                                ),
                                new = DependencyNode<FirProperty>::PropertyNode,
                                connect = { accesses(it, propertyAccessExpression) }
                            )
                        }
                    }
                }
            }

        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression): Unit {
            TODO("Not yet implemented")
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier): Unit =
            resolvedQualifier.visit {
                // Consider only objects, enum entries are accessible as properties (variables), and (static) classes are not accessible
                if (canBeValue) {
                    symbol?.fullyExpandedClass(session)?.apply {
                        // Case 1: a companion object
                        if (resolvedToCompanionObject) {
                            resolvedCompanionObjectSymbol?.asObjectEntity(asClassEntity())?.let { enclosingEntity ->
                                buildBeginInitializationNode(
                                    enclosingEntity = enclosingEntity.markFirstUse(),
                                    new = DependencyNode<FirRegularClass>::QualifierNode,
                                    connect = { accesses(it, resolvedQualifier) }
                                )
                            }
                        }
                        // Case 2: an object
                        else if (classKind.isObject) {
                            asObjectEntity()?.let { enclosingEntity ->
                                buildBeginInitializationNode(
                                    enclosingEntity = enclosingEntity.markFirstUse(),
                                    new = DependencyNode<FirRegularClass>::QualifierNode,
                                    connect = { accesses(it, resolvedQualifier) }
                                )
                            }
                        }
                    }
                }
            }

        override fun visitReturnExpression(returnExpression: FirReturnExpression): Unit = returnExpression.visit {
            acceptChildren(this@Builder)
        }

        override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression): Unit = safeCallExpression.visit {
            acceptChildren(this@Builder)
        }

        override fun visitSamConversionExpression(samConversionExpression: FirSamConversionExpression): Unit =
            samConversionExpression.visit { acceptChildren(this@Builder) }

        override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression): Unit =
            smartCastExpression.visit { acceptChildren(this@Builder) }

        override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression): Unit =
            spreadArgumentExpression.visit { acceptChildren(this@Builder) }

        override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall): Unit =
            stringConcatenationCall.visit { acceptChildren(this@Builder) }

        override fun visitThrowExpression(throwExpression: FirThrowExpression): Unit = throwExpression.visit {
            acceptChildren(this@Builder)
        }

        override fun visitTryExpression(tryExpression: FirTryExpression): Unit = tryExpression.visit {
            acceptChildren(this@Builder)
        }

        override fun visitWhenExpression(whenExpression: FirWhenExpression): Unit = whenExpression.visit {
            acceptChildren(this@Builder)
        }

        override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression): Unit = whenSubjectExpression.visit {
            acceptChildren(this@Builder)
        }

        override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression): Unit =
            wrappedArgumentExpression.visit { acceptChildren(this@Builder) }

        override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression): Unit =
            wrappedDelegateExpression.visit { acceptChildren(this@Builder) }

        override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression): Unit = wrappedExpression.visit {
            acceptChildren(this@Builder)
        }
    }
}

val FirSession.dependencyGraph: DependencyGraph by FirSession.sessionComponentAccessor()
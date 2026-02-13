/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DependencyNode.Companion.connectTo
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.accept
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.FirPropertyAccessorBuilder
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirBooleanOperatorExpression
import org.jetbrains.kotlin.fir.expressions.FirBreakExpression
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirContinueExpression
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirDesugaredAssignmentValueReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirErrorAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirInaccessibleReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirIncrementDecrementExpression
import org.jetbrains.kotlin.fir.expressions.FirIntegerLiteralOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirJump
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirLoopJump
import org.jetbrains.kotlin.fir.expressions.FirMultiDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedErrorAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirResolvedReifiedParameterReference
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSamConversionExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
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
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.Stack
import org.jetbrains.kotlin.fir.resolve.dfa.isEmpty
import org.jetbrains.kotlin.fir.resolve.dfa.isNotEmpty
import org.jetbrains.kotlin.fir.resolve.dfa.stackOf
import org.jetbrains.kotlin.fir.resolve.dfa.topOrNull
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// TODO: handle enum entries
/**
 * Represents the sources of static entities such as properties in objects or top-level properties
 */
sealed interface SourcePoint {

    val symbol: FirBasedSymbol<*>
    val properties: Set<FirPropertySymbol>

    data class Object(override val symbol: FirRegularClassSymbol) : SourcePoint {
        override val properties: Set<FirPropertySymbol> = symbol.declaredProperties(symbol.moduleData.session).toCollection(linkedSetOf())
    }

    data class File(override val symbol: FirFileSymbol) : SourcePoint {
        @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
        override val properties: Set<FirPropertySymbol> =
            symbol.fir.declarations.filterIsInstance<FirProperty>().mapTo(linkedSetOf()) { it.symbol }
    }
//    data class EnumEntry(override val symbol: FirEnumEntrySymbol) : SourcePoint

    companion object {
        fun fromFile(symbol: FirFileSymbol): SourcePoint = File(symbol)
        fun fromObject(symbol: FirRegularClassSymbol): SourcePoint {
            require(symbol.classKind.isObject)
            return Object(symbol)
        }
    }
}

sealed class DependencyNode {
    private val _parents: MutableSet<DependencyNode> = linkedSetOf()
    val parents: Set<DependencyNode> get() = _parents
    private val _children: MutableSet<DependencyNode> = linkedSetOf()
    val children: Set<DependencyNode> get() = _children

    sealed class IndexedNode<D : FirDeclaration> : DependencyNode() {
        abstract val symbol: FirBasedSymbol<D>
    }

    /**
     * Represents nodes that trigger access to their enclosing declaration called a source point (i.e., to a top-level property, an object property, or an enum entry property)
     *
     * The parent symbol represents the enclosing static entity (i.e., a file, a (companion) object, or an enum class)
     */
    sealed class AccessNode<D : FirDeclaration> : IndexedNode<D>() {
        abstract val source: SourcePoint
    }

    /**
     * Represents access to a static property (i.e., to a top-level property, an object property, or an enum entry property)
     */
    data class PropertyNode(override val symbol: FirPropertySymbol) : AccessNode<FirProperty>() {
        override val source: SourcePoint =
            symbol.getContainingClassSymbol()?.fullyExpandedClass(symbol.moduleData.session)?.let(SourcePoint::fromObject)
                ?: symbol.containingFileSymbol?.let(SourcePoint::fromFile)
                ?: error("No source point found for property: $symbol")

        val initializerNode: PropertyInitializerNode = PropertyInitializerNode(this)
    }

    data class PropertyInitializerNode(val propertyNode: PropertyNode) : DependencyNode()

    data class InitializerBlockNode(override val symbol: FirAnonymousInitializerSymbol) : AccessNode<FirAnonymousInitializer>() {
        override val source: SourcePoint =
            symbol.getContainingClassSymbol()?.fullyExpandedClass(symbol.moduleData.session)?.let(SourcePoint::fromObject)
                ?: error("No source point found for initializer block: $symbol")
    }

    data class FunctionCallNode<D : FirCallableDeclaration>(override val symbol: FirCallableSymbol<D>) : IndexedNode<D>()

    companion object {
        infix fun <T : DependencyNode> DependencyNode?.connectTo(node: T): T {
            // disallow self-loops
            if (node != this && this != null) {
                _children += node
                node._parents += this
            }
            return node
        }
    }
}

typealias CircularityInfo = Set<DependencyNode>

val CircularityInfo.symbols: Set<FirBasedSymbol<*>>
    get() = filterIsInstance<DependencyNode.IndexedNode<*>>().map(DependencyNode.IndexedNode<*>::symbol).toSet()

val CircularityInfo.sources: Set<SourcePoint>
    get() = filterIsInstance<DependencyNode.AccessNode<*>>().map(DependencyNode.AccessNode<*>::source).toSet()

class DependencyGraph {

    private val _nodes = mutableSetOf<DependencyNode>()
    private val symbolToNodes = mutableMapOf<FirBasedSymbol<*>, DependencyNode.IndexedNode<*>>()
    private val sourceToNodes = mutableMapOf<SourcePoint, MutableSet<DependencyNode.AccessNode<*>>>()
    val stack: Stack<DependencyNode> = stackOf()

    val nodes: Set<DependencyNode> get() = _nodes

    val sources: Set<SourcePoint> get() = sourceToNodes.keys

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun addDependencies(file: FirFile) {
        require(stack.isEmpty)
        file.accept(DependencyGraphBuilder, BuildingContext(context, reporter, this))
        require(stack.isEmpty)
    }

    /**
     * Returns whether the [propertyA] and [propertyB] satisfy the depends-on relation: [propertyA] => [propertyB]
     *
     * By definition, the initialization [propertyA] *depends on* [propertyA] ([propertyA] => [propertyB]) iff there exists a path from
     * [propertyA] to [propertyB] in the dependency graph
     */
    fun dependsOn(propertyA: FirBasedSymbol<*>, propertyB: FirPropertySymbol): Boolean {
        if (!symbolToNodes.containsKey(propertyA) || !symbolToNodes.containsKey(propertyB)) return false
        if (propertyA == propertyB) return true

        fun reach(node: DependencyNode, target: DependencyNode, visited: MutableSet<DependencyNode> = mutableSetOf()): Boolean {
            if (node == target) return true
            visited.add(node)
            return node.children.filter { it !in visited }.any { reach(it, target, visited) }
        }

        return reach(symbolToNodes.getValue(propertyA), symbolToNodes.getValue(propertyB))
    }

    /**
     * Returns whether the [s1] and [s2] satisfy the depends-on relation: [s1] => [s2]
     *
     * By definition, the initialization of [s1] *depends on* [s2] iff there exists a property in [s1] and a property in
     * [s2] such that the initialization of the property in [s1] depends on the property in [s2]
     */
    fun dependsOn(s1: SourcePoint, s2: SourcePoint): Boolean {
        if (!sourceToNodes.containsKey(s1) || !sourceToNodes.containsKey(s2)) return false
        if (s1 == s2) return true
        return s1.properties.any { p1 -> s2.properties.any { p2 -> dependsOn(p1, p2) } }
    }

    /**
     * Returns whether it is possible to incur a deadlock between the two source points
     *
     * For the source points (e.g., objects) are declared inside a specific file source point, and their properties depend on (one or more of)
     * its top-level properties, there is no possibility of deadlock. It is not even possible to deadlock during the cyclic initialization of
     * a file's top-level properties and an object that is *not* declared inside it, as it will be lazy-initialized in the top-level scope
     * of the file. However, it is possible to deadlock on the cyclic initialization of top-level properties in different files.
     */
    fun canDeadlock(s1: SourcePoint, s2: SourcePoint, inSameComponent: Boolean = false): Boolean {
        if (s1 == s2) return false
        if (s1 is SourcePoint.File && s2 !is SourcePoint.File) return false
        if (s2 is SourcePoint.File && s1 !is SourcePoint.File) return false
        return inSameComponent || dependsOn(s1, s2) && dependsOn(s2, s1)
    }

    private inline fun <D : FirDeclaration, S : FirBasedSymbol<D>, reified T : DependencyNode.IndexedNode<D>> addIndexedNode(
        symbol: S,
        connectToCurrent: Boolean = true,
        crossinline init: (S) -> T,
        crossinline block: (T) -> Unit
    ): T = addNode(symbolToNodes[symbol] as? T ?: init(symbol).apply { symbolToNodes[symbol] = this }, connectToCurrent, block)

    private inline fun <T : DependencyNode> addNode(
        node: T,
        connectToCurrent: Boolean = true,
        crossinline block: (T) -> Unit
    ): T {
        if (!_nodes.add(node)) {
            if (connectToCurrent) stack.topOrNull() connectTo node
            return node
        }
        if (node is DependencyNode.AccessNode<*>) sourceToNodes.merge(
            node.source,
            mutableSetOf(node),
            MutableSet<DependencyNode.AccessNode<*>>::join
        )
        stack.push(if (connectToCurrent) stack.topOrNull() connectTo node else node)
        try {
            block(node)
        } finally {
            stack.pop()
        }
        return node
    }

    /**
     * Computes a list of circularities in the dependency graph aka a list of strongly connected components.
     */
    fun computeCircularities(): Set<CircularityInfo> {
        val visited = mutableSetOf<DependencyNode>()
        val sorted = stackOf<DependencyNode>()
        val result = mutableSetOf<CircularityInfo>()

        fun DependencyNode.topoSort() {
            if (visited.add(this)) {
                children.forEach(DependencyNode::topoSort)
                sorted.push(this)
            }
        }

        nodes.forEach(DependencyNode::topoSort)
        visited.clear()

        while (sorted.isNotEmpty) {
            val current = sorted.pop()
            if (visited.add(current)) {
                val component = mutableSetOf<DependencyNode>()

                fun DependencyNode.visitParents() {
                    component += this
                    parents.forEach { if (visited.add(it)) it.visitParents() }
                }

                current.visitParents()
                result += component
            }
        }

        return result
    }

    override fun toString(): String = nodes.toString()

    private data class BuildingContext(
        val diagnosticContext: CheckerContext,
        val diagnosticReporter: DiagnosticReporter,
        val graph: DependencyGraph
    )

    // TODO: figure out how to handle every possible expression and declaration correctly
    // TODO: inheritance and delegation support
    @OptIn(SymbolInternals::class)
    private object DependencyGraphBuilder : FirVisitor<Unit, BuildingContext>() {

        private inline fun visit(
            context: BuildingContext,
            crossinline block: context(BuildingContext, CheckerContext, DiagnosticReporter, DependencyGraph) () -> Unit
        ) = block(context, context.diagnosticContext, context.diagnosticReporter, context.graph)

        context(graph: DependencyGraph)
        private inline fun FirPropertySymbol.addProperty(
            connectToCurrent: Boolean = true,
            crossinline block: (DependencyNode.PropertyNode) -> Unit = {}
        ): DependencyNode.PropertyNode = graph.addIndexedNode(this, connectToCurrent, DependencyNode::PropertyNode, block)

        context(graph: DependencyGraph)
        private inline fun DependencyNode.PropertyNode.addPropertyInitializer(
            connectToCurrent: Boolean = true,
            crossinline block: (DependencyNode.PropertyInitializerNode) -> Unit = {}
        ): DependencyNode.PropertyInitializerNode = graph.addNode(initializerNode, connectToCurrent, block)

        context(graph: DependencyGraph)
        private inline fun FirAnonymousInitializerSymbol.addInitializerBlock(
            connectToCurrent: Boolean = true,
            crossinline block: (DependencyNode.InitializerBlockNode) -> Unit = {}
        ): DependencyNode.InitializerBlockNode = graph.addIndexedNode(this, connectToCurrent, DependencyNode::InitializerBlockNode, block)

        context(graph: DependencyGraph)
        private inline fun <D : FirCallableDeclaration> FirCallableSymbol<D>.addFunctionCall(
            connectToCurrent: Boolean = true,
            crossinline block: (DependencyNode.FunctionCallNode<D>) -> Unit = {}
        ): DependencyNode.FunctionCallNode<D> = graph.addIndexedNode(this, connectToCurrent, DependencyNode::FunctionCallNode, block)

        context(data: BuildingContext, graph: DependencyGraph)
        private fun FirRegularClass.addObject(connectFrom: FirPropertySymbol? = null) {
            // TODO: handle enum entries
            // keep track of object property initializer nodes and initializer block nodes to establish order-of-declaration dependencies
            val alreadyDeclaredNodes = mutableListOf<DependencyNode>()
            var hasReachedProperty = connectFrom == null
            processAllDeclarations(moduleData.session) { symbol ->
                when (symbol) {
                    is FirPropertySymbol -> {
                        hasReachedProperty = hasReachedProperty || (symbol == connectFrom)
                        symbol.addProperty(connectToCurrent = hasReachedProperty) { node ->
                            alreadyDeclaredNodes.forEach { node connectTo it }
                            node.addPropertyInitializer {
                                symbol.fir.initializer?.accept(this@DependencyGraphBuilder)
                                    ?: symbol.fir.getter?.accept(this@DependencyGraphBuilder)
                            }
                        }.let { alreadyDeclaredNodes += it.initializerNode }
                    }
                    is FirAnonymousInitializerSymbol -> symbol.addInitializerBlock(connectToCurrent = hasReachedProperty) { node ->
                        alreadyDeclaredNodes.forEach { node connectTo it }
                        symbol.fir.accept(this@DependencyGraphBuilder)
                    }.let { alreadyDeclaredNodes += it }
                    else -> {}
                }
            }
        }

        @OptIn(DirectDeclarationsAccess::class)
        context(data: BuildingContext, graph: DependencyGraph)
        private fun FirFile.addFile(
            connectAt: FirPropertySymbol? = null,
            inDifferentScope: Boolean = false,
            initializeClasses: Boolean = true
        ) {
            // keep track of top-level property initializer nodes to establish order-of-declaration dependencies
            val alreadyDeclaredNodes = mutableListOf<DependencyNode.PropertyInitializerNode>()
            var hasReachedProperty = connectAt == null
            declarations.forEach { declaration ->
                when (declaration) {
                    is FirProperty -> {
                        hasReachedProperty = hasReachedProperty || (connectAt == declaration.symbol)
                        declaration.symbol.addProperty(
                            connectToCurrent = inDifferentScope && hasReachedProperty || !inDifferentScope && connectAt == declaration.symbol,
                        ) { node ->
                            alreadyDeclaredNodes.forEach { node connectTo it }
                            node.addPropertyInitializer {
                                declaration.initializer?.accept(this@DependencyGraphBuilder)
                                    ?: declaration.getter?.accept(this@DependencyGraphBuilder)
                            }
                        }.let { alreadyDeclaredNodes += it.initializerNode }
                    }
                    is FirRegularClass if initializeClasses -> declaration.accept(this@DependencyGraphBuilder)
                    else -> {}
                }
            }
        }

        override fun visitElement(element: FirElement, data: BuildingContext): Unit = Unit

        /**
         * =============================================
         *                  DECLARATIONS
         * =============================================
         */


        override fun visitFile(file: FirFile, data: BuildingContext) = visit(data) {
            file.addFile()
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: BuildingContext) = visit(data) {
            // TODO: handle enum entries
            val regularClass = regularClass.symbol.resolvedCompanionObjectSymbol?.fir ?: regularClass
            if (regularClass.classKind.isObject) {
                regularClass.addObject()
            }
        }

        override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: BuildingContext) = visit(data) {
            anonymousInitializer.acceptChildren(this)
        }

        override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: BuildingContext) = visit(data) {
            propertyAccessor.acceptChildren(this)
        }

        /**
         * =============================================
         *                  EXPRESSIONS
         * =============================================
         */

        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitBlock(block: FirBlock, data: BuildingContext) = visit(data) {
            block.acceptChildren(this)
        }

        override fun visitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression, data: BuildingContext) =
            visit(data) {
                booleanOperatorExpression.acceptChildren(this)
            }

        override fun visitBreakExpression(breakExpression: FirBreakExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: BuildingContext) =
            visit(data) {
                callableReferenceAccess.acceptChildren(this)
            }

        override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: BuildingContext) = visit(data) {
            checkedSafeCallSubject.originalReceiverRef.value.accept(this)
        }

        override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: BuildingContext) = visit(data) {
            checkNotNullCall.acceptChildren(this)
        }

        override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: BuildingContext) =
            // ::class expression initializes the class object which in turn initializes the class statically
            visit(data) {
//                classReferenceExpression.classTypeRef.coneType.toClassLikeSymbol()?.fullyExpandedClass()?.let { klass ->
//                    // TODO: handle enum entries
//                    val klass = klass.resolvedCompanionObjectSymbol ?: klass.companionObjectSymbol ?: klass
//                    if (klass.classKind.isObject) {
//                        klass.fir.addObject()
//                    }
//                }
            }

        override fun visitCollectionLiteral(collectionLiteral: FirCollectionLiteral, data: BuildingContext) = visit(data) {
            collectionLiteral.acceptChildren(this)
        }

        override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: BuildingContext) = visit(data) {
            comparisonExpression.acceptChildren(this)
        }

        override fun visitComponentCall(componentCall: FirComponentCall, data: BuildingContext): Unit = visit(data) {
            componentCall.toResolvedCallableSymbol()?.let { symbol ->
                symbol.addFunctionCall { symbol.fir.accept(this) }
            }
        }

        override fun visitContinueExpression(continueExpression: FirContinueExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitDesugaredAssignmentValueReferenceExpression(
            desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression,
            data: BuildingContext
        ) = visit(data) {
            desugaredAssignmentValueReferenceExpression.expressionRef.value.accept(this)
        }

        override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: BuildingContext) = visit(data) {
            elvisExpression.acceptChildren(this)
        }

        override fun visitEnumEntryDeserializedAccessExpression(
            enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression,
            data: BuildingContext
        ) = visit(data) {
            enumEntryDeserializedAccessExpression.resolvedType.toClassLikeSymbol()?.fullyExpandedClass()?.collectEnumEntries()
                ?.firstOrNull { it.name == enumEntryDeserializedAccessExpression.enumEntryName }?.let { symbol ->
                    // TODO: handle enum entries
                }
        }

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: BuildingContext) = visit(data) {
            TODO("Not yet implemented")
        }

        override fun visitErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitErrorExpression(errorExpression: FirErrorExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall, data: BuildingContext) = visit(data) {
            functionCall.argumentList.acceptChildren(this)
            functionCall.calleeReference.toResolvedFunctionSymbol()?.let { symbol ->
                symbol.addFunctionCall { symbol.fir.accept(this) }
            }
        }

        override fun visitGetClassCall(getClassCall: FirGetClassCall, data: BuildingContext) = visit(data) {
            // should forward to ClassReferenceExpression
//            getClassCall.acceptChildren(this)
        }

        override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: BuildingContext) = visit(data) {
            implicitInvokeCall.argumentList.acceptChildren(this)
            implicitInvokeCall.calleeReference.toResolvedFunctionSymbol()?.fir?.accept(this)
        }

        override fun visitInaccessibleReceiverExpression(
            inaccessibleReceiverExpression: FirInaccessibleReceiverExpression,
            data: BuildingContext
        ) {
            TODO("Not yet implemented")
        }

        override fun visitIncrementDecrementExpression(
            incrementDecrementExpression: FirIncrementDecrementExpression,
            data: BuildingContext
        ) = visit(data) {
            incrementDecrementExpression.acceptChildren(this)
        }

        override fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: BuildingContext) =
            visit(data) {
                integerLiteralOperatorCall.argumentList.acceptChildren(this)
                integerLiteralOperatorCall.calleeReference.toResolvedFunctionSymbol()?.let { symbol ->
                    symbol.addFunctionCall { symbol.fir.accept(this) }
                }
            }

        override fun <E : FirTargetElement> visitJump(jump: FirJump<E>, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitLazyBlock(lazyBlock: FirLazyBlock, data: BuildingContext) = visit(data) {
            lazyBlock.acceptChildren(this)
        }

        override fun visitLazyExpression(lazyExpression: FirLazyExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitLiteralExpression(literalExpression: FirLiteralExpression, data: BuildingContext) = Unit

        override fun visitLoopJump(loopJump: FirLoopJump, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitMultiDelegatedConstructorCall(
            multiDelegatedConstructorCall: FirMultiDelegatedConstructorCall,
            data: BuildingContext
        ) {
            TODO("Not yet implemented")
        }

        override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: BuildingContext) =
            visit(data) {
                namedArgumentExpression.acceptChildren(this)
            }

        @OptIn(DirectDeclarationsAccess::class)
        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: BuildingContext) =
            visit(data) {
                propertyAccessExpression.calleeReference.toResolvedPropertySymbol()?.let { symbol ->
                    symbol.getContainingClassSymbol()?.fullyExpandedClass()?.let { klass ->
                        // TODO: handle enum entries
                        if (klass.classKind.isObject) {
                            klass.fir.addObject(connectFrom = symbol)
                        }
                    } ?: symbol.containingFileSymbol?.let { file ->
                        // if the containing file symbol is different from the one being currently checked,
                        // we have to connect to all its declared properties
                        val accessingDifferentFile = data.diagnosticContext.containingFileSymbol?.let { it != file } ?: false
                        file.fir.addFile(
                            connectAt = symbol,
                            inDifferentScope = accessingDifferentFile,
                            initializeClasses = accessingDifferentFile
                        )
                    }
                }
            }

        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitQualifiedErrorAccessExpression(
            qualifiedErrorAccessExpression: FirQualifiedErrorAccessExpression,
            data: BuildingContext
        ) {
            TODO("Not yet implemented")
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: BuildingContext) = visit(data) {
            // when accessing the qualifier, we connect the current node on the stack to all the properties declared in the qualified object
//            resolvedQualifier.symbol?.fullyExpandedClass()?.fir?.addObject()
        }

        override fun visitResolvedReifiedParameterReference(
            resolvedReifiedParameterReference: FirResolvedReifiedParameterReference,
            data: BuildingContext
        ) {
            TODO("Not yet implemented")
        }

        override fun visitReturnExpression(returnExpression: FirReturnExpression, data: BuildingContext) = visit(data) {
            returnExpression.acceptChildren(this)
        }

        override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitSamConversionExpression(samConversionExpression: FirSamConversionExpression, data: BuildingContext) =
            visit(data) {
                samConversionExpression.acceptChildren(this)
            }

        override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: BuildingContext) {
            // TODO: on function calls on the original expression, add function nodes that symbolize the overridden function as well
            TODO("Not yet implemented")
        }

        override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: BuildingContext) =
            visit(data) {
                spreadArgumentExpression.acceptChildren(this)
            }

        override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitSuperReceiverExpression(superReceiverExpression: FirSuperReceiverExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: BuildingContext) {
            // TODO: add the function call node here
            TODO("Not yet implemented")
        }

        override fun visitThrowExpression(throwExpression: FirThrowExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitTryExpression(tryExpression: FirTryExpression, data: BuildingContext) = visit(data) {
            tryExpression.acceptChildren(this)
        }

        override fun visitWhenExpression(whenExpression: FirWhenExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: BuildingContext) {
            TODO("Not yet implemented")
        }

        /**
         * =============================================
         *                  REFERENCES
         * =============================================
         */

        override fun visitNamedReference(namedReference: FirNamedReference, data: BuildingContext) = visit(data) {
            namedReference.toResolvedCallableSymbol()?.fir?.accept(this)
        }

        override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: BuildingContext) = visit(data) {
            resolvedNamedReference.toResolvedCallableSymbol()?.fir?.accept(this)
        }
    }
}

typealias FinderData = Pair<FirPropertySymbol, (FirExpression) -> Unit>

private object PropertyAccessorFinder : FirDefaultVisitor<Unit, FinderData>() {
    override fun visitElement(element: FirElement, data: FinderData) {
        when (element) {
            is FirPropertyAccessExpression if element.calleeReference.toResolvedPropertySymbol() == data.first -> {
                data.second(element)
            }
            else -> element.acceptChildren(this, data)
        }
    }
}

@OptIn(SymbolInternals::class)
fun FirBasedSymbol<*>.forEachPropertyAccessTo(symbol: FirPropertySymbol, block: (FirExpression) -> Unit) {
    val result: FinderData = symbol to block
    fir.accept(PropertyAccessorFinder, result)
}

// TODO: inheritance and delegation
object FirStaticInitializationChecker : FirFileChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        // 1. Construct the dependency graph
        val graph = DependencyGraph()
        graph.addDependencies(declaration)

        // 2. Check for `bad circularities` from the strongly connected components of the dependency graph
        val checkedSources = mutableSetOf<SourcePoint>()
        // Filter out trivial strongly connected components first
        graph.computeCircularities().filter { it.size > 1 }.forEach { info ->
            val symbols = info.symbols
            val sources = info.sources
            sources.forEach { source ->
                // Report warnings of possible deadlocks at individual source points, since they are a part of a strongly connected component
                sources.filter { graph.canDeadlock(source, it, inSameComponent = true) }.forEach { other ->
                    reporter.reportOn(source.symbol.source, FirErrors.POSSIBLE_DEADLOCK, other.symbol)
                }
                // Report warnings of uninitialized access errors at specific properties
                source.properties.filter { it in symbols }.forEach { prop ->
                    reporter.reportOn(prop.source, FirErrors.UNINITIALIZED_PROPERTY)
                    symbols.filter { it != prop }.forEach { other ->
                        other.forEachPropertyAccessTo(prop) {
                            reporter.reportOn(it.source, FirErrors.UNINITIALIZED_ACCESS, prop)
                        }
                    }
                }
            }
            checkedSources += sources
        }

        // 3. Report warning of possible deadlocks at source points where the accesses are cyclic but initialized
        val remainingSources = graph.sources - checkedSources
        remainingSources.forEach { source ->
            remainingSources.filter { it !in checkedSources && graph.canDeadlock(source, it) }.forEach { other ->
                reporter.reportOn(source.symbol.source, FirErrors.POSSIBLE_DEADLOCK, other.symbol)
                reporter.reportOn(other.symbol.source, FirErrors.POSSIBLE_DEADLOCK, source.symbol)
            }
            checkedSources += source
        }
    }
}

private fun <E> MutableSet<E>.join(other: Iterable<E>): MutableSet<E> = apply {
    other.forEach { add(it) }
}

context(data: D)
private fun <R, D> FirElement.accept(visitor: FirVisitor<R, D>): R = accept(visitor, data)

context(data: D)
private fun <R, D> FirElement.acceptChildren(visitor: FirVisitor<R, D>): Unit = acceptChildren(visitor, data)

private fun FirClassSymbol<*>.collectEnumEntries(): List<FirEnumEntrySymbol> {
    return collectEnumEntries(moduleData.session)
}

private val FirBasedSymbol<*>.containingFileSymbol: FirFileSymbol? get() = moduleData.session.firProvider.getContainingFile(this)?.symbol

private fun <T> cachedSequence(block: suspend SequenceScope<T>.() -> Unit) = object : Sequence<T> {
    val cache = mutableListOf<T>()
    val oneShotIterator = iterator(block)

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        private var index = 0

        override fun hasNext(): Boolean = index < cache.size || oneShotIterator.hasNext()

        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException()
            if (oneShotIterator.hasNext() && index >= cache.size) {
                cache += oneShotIterator.next()
            }
            return cache[index++]
        }
    }
}

private operator fun <T> Set<T>.times(other: Set<T>): Set<T> = Intersection(this, other)

private operator fun <T> Set<T>.minus(other: Set<T>): Set<T> = Difference(this, other)

internal data class Intersection<T>(val s1: Set<T>, val s2: Set<T>) : Set<T> {
    private val elements = cachedSequence {
        s1.forEach { if (it in s2) yield(it) }
    }

    override val size: Int get() = elements.count()

    override fun isEmpty(): Boolean = !iterator().hasNext()

    override fun contains(element: T): Boolean = s1.contains(element) && s2.contains(element)

    override fun iterator(): Iterator<T> = elements.iterator()

    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }
}

internal data class Difference<T>(val s1: Set<T>, val s2: Set<T>) : Set<T> {
    private val elements = cachedSequence {
        s1.forEach { if (it !in s2) yield(it) }
    }

    override val size: Int get() = elements.count()

    override fun isEmpty(): Boolean = !iterator().hasNext()

    override fun contains(element: T): Boolean = s1.contains(element) && !s2.contains(element)

    override fun iterator(): Iterator<T> = elements.iterator()

    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }
}
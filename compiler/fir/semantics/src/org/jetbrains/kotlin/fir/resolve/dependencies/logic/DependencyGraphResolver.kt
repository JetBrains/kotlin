/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies.logic

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.isGeneratedStaticEnumMember
import org.jetbrains.kotlin.fir.resolve.dependencies.ClinitIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.DeclarationIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraph
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyNodeIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asClassEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asEnumEntryEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asFileEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asObjectEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnumEntryIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.QualifierIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.StaticAnonymousInitializerIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.StaticPropertyIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.TopLevelIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.dsl.DependencyGraphBuilder
import org.jetbrains.kotlin.fir.resolve.dependencies.dsl.DependencyNodeBuilder
import org.jetbrains.kotlin.fir.resolve.dependencies.dsl.DependencySubgraphBuilder
import org.jetbrains.kotlin.fir.resolve.dependencies.dsl.buildGraph
import org.jetbrains.kotlin.fir.resolve.dependencies.inSameModule
import org.jetbrains.kotlin.fir.resolve.dependencies.isInitializedBySupertypes
import org.jetbrains.kotlin.fir.resolve.dependencies.isLibraryDeclaration
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.util.BaseMultimap
import java.util.LinkedList
import kotlin.collections.forEach
import kotlin.sequences.forEach

class DependencyGraphResolver(val dependencyGraph: DependencyGraph) : FirSessionComponent {

    private val visitedFiles = mutableSetOf<FirFileSymbol>()

    private val worklist = LinkedList<DependencyNodeIndex>()

    private val processed = mutableSetOf<DependencyNodeIndex>()

    private val pendingNodes =
        object : BaseMultimap<FirFileSymbol, DependencyNodeIndex, List<DependencyNodeIndex>, MutableList<DependencyNodeIndex>>() {
            override fun createContainer(): MutableList<DependencyNodeIndex> = LinkedList()
            override fun createEmptyContainer(): List<DependencyNodeIndex> = emptyList()
        }

    /**
     * Connects the subgraph of this entity (at its begin node) with an incoming happens-before edge to the subgraphs of its supertypes
     *
     * The supertypes connected to this entity are those which are directly initialized due to initialization of this entity, i.e.,
     * classes and interfaces with default methods. In cases where the supertype has no static declarations, we recurse into its supertypes
     * to and connect to those instead
     */
    context(holder: SessionAndScopeSessionHolder)
    private fun DependencySubgraphBuilder<*>.initializeDirectSupertypes(classSymbol: FirClassSymbol<*>) {
        classSymbol.resolvedSuperTypes.forEach { superType ->
            val symbol = superType.fullyExpandedType().toRegularClassSymbol() ?: return@forEach
            // Skip library supertypes, as they cannot have mutual dependencies with the source types, interface types without
            // default methods, and types which are declared outside the current module
            if (symbol.isLibraryDeclaration
                || !symbol.isInitializedBySupertypes
                || !symbol.inSameModule()
            ) return
            val supertypeEntity = symbol.asClassEntity()
            // We do not need to visit the supertype here, as it was either already visited
            // or will be visited later (in this file or in a subseqently visited one)
            val endNode = supertypeEntity.endInitializationIndex
            endNode.buildNode()
            endNode mustHappenBefore enclosingEntity.beginInitializationIndex
        }
    }

    /**
     * Retrieves all declarations that are initialized in order of declaration and in the order of initialization of the given class'
     * supertypes.
     *
     * For the sake of optimization, the resulting sequence excludes all library declarations that have been overridden by the given
     * class (or transitively by its supertypes)
     */
    context(holder: SessionAndScopeSessionHolder)
    private fun FirClassSymbol<*>.collectInitializedDeclarations(): Sequence<FirBasedSymbol<*>> = sequence {
        // Prevent visiting classes that belong to a library, belong to a different module, are interfaces, or are annotation classes
        if (isLibraryDeclaration || inSameModule().not() || classKind == ClassKind.INTERFACE || classKind == ClassKind.ANNOTATION_CLASS) return@sequence

        // Populate the declared declarations (properties and init blocks), and overridden properties
        // The declarations are collected recursively, respecting JVM's initialization rules
        // JVMS25 (5.5.7):
        // ... if C is a class rather than an interface, then let SC be its superclass and let SI1, ..., SIn be all superinterfaces of C
        // (whether direct or indirect) that declare at least one non-abstract, non-static method. The order of superinterfaces is given
        // by a recursive enumeration over the superinterface hierarchy of each interface directly implemented by C. For each interface I
        // directly implemented by C (in the order of the interfaces array of C), the enumeration recurs on I's superinterfaces (in the
        // order of the interfaces array of I) before returning I. ...

        // We do not need to enumerate the supertype hierarchy recursively because it is equivalent as recursively calling the cache
        // on each directly implemented supertype
        val superClass = resolvedSuperTypes.asSequence().mapNotNull { it.fullyExpandedType().toRegularClassSymbol() }
            .firstOrNull { it.classKind == ClassKind.CLASS || it.classKind == ClassKind.ENUM_CLASS }
        superClass?.let { yieldAll(it.collectInitializedDeclarations()) }

        yieldAll(declarationSymbols.asSequence().filter {
            it is FirPropertySymbol && it.hasInitializer || it is FirAnonymousInitializerSymbol
        })
    }

    context(holder: SessionAndScopeSessionHolder)
    private fun DependencyNodeBuilder.postponeClassEntity(
        classSymbol: FirRegularClassSymbol,
        outerEnclosingEntity: EnclosingEntity.Class? = null
    ) {
        val enclosingEntity = when {
            classSymbol.classKind.isObject -> classSymbol.asObjectEntity(outerEnclosingEntity)
            else -> classSymbol.asClassEntity()
        } ?: return
        worklist.add(enclosingEntity.beginInitializationIndex)
    }

    context(holder: SessionAndScopeSessionHolder)
    private fun DependencyNodeBuilder.postponeClassEntity(
        classDeclaration: FirRegularClass,
        outerEnclosingEntity: EnclosingEntity.Class? = null
    ) = postponeClassEntity(classDeclaration.symbol, outerEnclosingEntity)

    context(holder: SessionAndScopeSessionHolder)
    fun collectDependencies(file: FirFile): List<DependencyNodeIndex> {
        // Skip files outside the current module and already visited files
        if (!file.inSameModule() || !visitedFiles.add(file.symbol)) return pendingNodes.removeKey(file.symbol)

        dependencyGraph.buildGraph(worklist) {
            val callSiteVisitor = CallSiteVisitor(
                session = holder.session,
                scopeSession = holder.scopeSession,
                visitedFiles = visitedFiles,
                graphBuilder = this
            )

            // Collect reachable roots from the given file
            buildFileEntity(file)

            while (worklist.isNotEmpty()) {
                val current = worklist.removeFirst()
                if (processed.add(current)) {
                    when (current) {
                        is TopLevelIndex if visitedFiles.add(current.enclosingEntity.symbol) -> buildFileEntity(current.enclosingEntity.symbol.fir)
                        is ClinitIndex -> {
                            buildClassEntity(current.enclosingEntity)
                            val containingFile = current.containingFile ?: continue
                            pendingNodes.put(containingFile, current)
                        }
                        is QualifierIndex -> {
                            buildObjectEntity(current.enclosingEntity)
                            val constructor = current.enclosingEntity.symbol.primaryConstructorIfAny(holder.session)?.fir ?: continue
                            constructor.accept(callSiteVisitor, CallSiteVisitor.CallSiteVisitContext(current, true))
                            val containingFile = current.containingFile ?: continue
                            pendingNodes.put(containingFile, current)
                        }
                        is EnumEntryIndex -> {
                            val constructor = current.enclosingEntity.symbol
                                .initializerObjectSymbol
                                ?.primaryConstructorIfAny(holder.session)
                                ?.fir
                                ?: continue
                            constructor.accept(callSiteVisitor, CallSiteVisitor.CallSiteVisitContext(current, true))
                            val containingFile = current.containingFile ?: continue
                            pendingNodes.put(containingFile, current)
                        }
                        is StaticPropertyIndex -> {
                            val propertySymbol = current.symbol
                            propertySymbol.fir.accept(callSiteVisitor, CallSiteVisitor.CallSiteVisitContext(current))
                            val containingDeclaration = holder.session.firProvider.getContainingClass(propertySymbol)
                                ?: holder.session.firProvider.getContainingFile(propertySymbol)?.symbol
                            if (containingDeclaration == current.enclosingEntity.symbol) {
                                val containingFile = current.containingFile ?: continue
                                pendingNodes.put(containingFile, current)
                            }
                        }
                        is StaticAnonymousInitializerIndex -> {
                            val initializedSymbol = current.symbol
                            current.symbol.fir.accept(callSiteVisitor, CallSiteVisitor.CallSiteVisitContext(current))
                            if (holder.session.firProvider.getContainingClass(initializedSymbol) == current.enclosingEntity.symbol) {
                                val containingFile = current.containingFile ?: continue
                                pendingNodes.put(containingFile, current)
                            }
                        }
                        is DeclarationIndex<*> -> {
                            current.symbol.fir.accept(callSiteVisitor, CallSiteVisitor.CallSiteVisitContext(current))
                        }
                        else -> {}
                    }
                }
            }

            // Condense the graph
            condenseGraph()
        }

        return pendingNodes.removeKey(file.symbol)
    }

    context(holder: SessionAndScopeSessionHolder)
    private fun DependencyGraphBuilder.buildFileEntity(file: FirFile) {
        val enclosingEntity = file.symbol.asFileEntity()
        enclosingEntity.buildSubgraph {
            // Keep track of which node has been previously constructed as properties and functions reside in different branches
            file.declarations.forEach { declaration ->
                when (declaration) {
                    is FirProperty -> buildProperty(declaration.symbol)
                    // JVM initialization of the file's class does not initialize the classes declared inside it.
                    // Hence, they will not be connected to the file's happens-before subgraph
                    is FirRegularClass -> postponeClassEntity(declaration)
                    else -> {}
                }
            }
        }
    }

    context(holder: SessionAndScopeSessionHolder)
    private fun DependencySubgraphBuilder<EnclosingEntity.Object>.buildObjectSubgraphNodes() {
        val objectSymbol = enclosingEntity.symbol
        initializeDirectSupertypes(objectSymbol)
        // Build all initialized declarations (declared or inherited)
        objectSymbol.collectInitializedDeclarations().forEach {
            when (it) {
                is FirPropertySymbol -> buildProperty(it)
                is FirAnonymousInitializerSymbol -> buildAnonymousInitializer(it)
                else -> {}
            }
        }
        // Find entities declared inside it
        objectSymbol.processAllDeclarations(holder.session) { symbol ->
            when (symbol) {
                is FirRegularClassSymbol -> postponeClassEntity(symbol)
                else -> {}
            }
        }
    }

    context(holder: SessionAndScopeSessionHolder)
    private fun DependencyGraphBuilder.buildObjectEntity(enclosingEntity: EnclosingEntity.Object) = enclosingEntity.buildSubgraph {
        buildObjectSubgraphNodes()
    }

    context(holder: SessionAndScopeSessionHolder)
    private fun DependencySubgraphBuilder<EnclosingEntity.Class>.buildCompanionObjectEntity(companionObject: EnclosingEntity.Object) =
        companionObject.buildNestedSubgraph {
            buildObjectSubgraphNodes()
        }

    context(holder: SessionAndScopeSessionHolder)
    private fun DependencyGraphBuilder.buildClassEntity(enclosingEntity: EnclosingEntity.Class) {
        val classSymbol = enclosingEntity.symbol
        enclosingEntity.buildSubgraph {
            initializeDirectSupertypes(classSymbol)
            // Store the companion object declaration just in case it appears in the declaration list before enum entries, due to serialization
            var companionObjectEntity: EnclosingEntity.Object? = null
            enclosingEntity.symbol.processAllDeclarations(holder.session) { symbol ->
                when (symbol) {
                    is FirPropertySymbol if symbol.fir.isGeneratedStaticEnumMember(classSymbol.fir) -> buildProperty(symbol)
                    is FirEnumEntrySymbol -> buildEnumEntryEntity(symbol.asEnumEntryEntity())
                    // Only companion objects will connect to this happens-before subgraph
                    is FirRegularClassSymbol if symbol.classKind.isObject && symbol.isCompanion ->
                        companionObjectEntity = symbol.asObjectEntity(enclosingEntity)
                    is FirRegularClassSymbol -> postponeClassEntity(symbol, enclosingEntity)
                    else -> {}
                }
            }
            // Build the companion object subgraph according to the declaration order of Kotlin's JVM compilation
            companionObjectEntity?.let { buildCompanionObjectEntity(it) }
        }
    }

    context(holder: SessionAndScopeSessionHolder)
    private fun DependencySubgraphBuilder<EnclosingEntity.Class>.buildEnumEntryEntity(enclosingEntity: EnclosingEntity.EnumEntry) {
        require(enclosingEntity.parentEnclosingEntity == this@buildEnumEntryEntity.enclosingEntity) { "The provided outer entity must match the enum entry's parent!" }
        enclosingEntity.buildNestedSubgraph {
            val symbol = enclosingEntity.symbol.initializerObjectSymbol ?: enclosingEntity.parentEnclosingEntity.symbol
            symbol.collectInitializedDeclarations().forEach {
                when (it) {
                    is FirPropertySymbol -> buildProperty(it)
                    is FirAnonymousInitializerSymbol -> buildAnonymousInitializer(it)
                    // NOTE: no classifiers should be accessible from an enum entry's anonymous object, as the enum entry
                    //  has the type of its (parent) enum class
                    else -> {}
                }
            }
        }
    }

    private fun DependencySubgraphBuilder<*>.buildProperty(propertySymbol: FirPropertySymbol) {
        if (!propertySymbol.isLocal && propertySymbol.isVal && propertySymbol.hasInitializer) {
            StaticPropertyIndex(enclosingEntity, propertySymbol).buildSubgraphNode()
        }
    }

    private fun DependencySubgraphBuilder<*>.buildAnonymousInitializer(initializerSymbol: FirAnonymousInitializerSymbol) =
        StaticAnonymousInitializerIndex(enclosingEntity, initializerSymbol).buildSubgraphNode()

    fun clear() {
        visitedFiles.clear()
        processed.clear()
    }
}

val FirSession.dependencyGraphResolver: DependencyGraphResolver? by FirSession.nullableSessionComponentAccessor()



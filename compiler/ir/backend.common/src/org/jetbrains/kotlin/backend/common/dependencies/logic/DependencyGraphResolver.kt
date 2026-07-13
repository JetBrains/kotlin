/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.logic

import org.jetbrains.kotlin.backend.common.dependencies.AnonymousInitializerIndex
import org.jetbrains.kotlin.backend.common.dependencies.BeginInstanceInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.ClinitIndex
import org.jetbrains.kotlin.backend.common.dependencies.DeclarationIndex
import org.jetbrains.kotlin.backend.common.dependencies.DependencyGraph
import org.jetbrains.kotlin.backend.common.dependencies.DependencyNodeIndex
import org.jetbrains.kotlin.backend.common.dependencies.EnumEntryIndex
import org.jetbrains.kotlin.backend.common.dependencies.PropertyIndex
import org.jetbrains.kotlin.backend.common.dependencies.QualifierIndex
import org.jetbrains.kotlin.backend.common.dependencies.TopLevelIndex
import org.jetbrains.kotlin.backend.common.dependencies.dsl.ClassSubgraphBuilder
import org.jetbrains.kotlin.backend.common.dependencies.dsl.DependencyGraphBuilder
import org.jetbrains.kotlin.backend.common.dependencies.dsl.DependencyNodeBuilder
import org.jetbrains.kotlin.backend.common.dependencies.dsl.ObjectSubgraphBuilder
import org.jetbrains.kotlin.backend.common.dependencies.dsl.StaticInitializationSubgraphBuilder
import org.jetbrains.kotlin.backend.common.dependencies.dsl.buildGraph
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.asClassEntity
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.asEnumEntryEntity
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.asFileEntity
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.asObjectEntity
import org.jetbrains.kotlin.backend.common.dependencies.util.BaseMultimap
import org.jetbrains.kotlin.backend.common.dependencies.util.contains
import org.jetbrains.kotlin.backend.common.dependencies.util.endInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.util.isInitializedBySupertypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.superClass
import java.util.LinkedList
import kotlin.collections.forEach
import kotlin.sequences.forEach

class DependencyGraphResolver(val dependencyGraph: DependencyGraph) {

    private val module get() = dependencyGraph.module

    private val visitedFiles = mutableSetOf<IrFileSymbol>()

    private val worklist = LinkedList<DependencyNodeIndex>()

    private val processed = mutableSetOf<DependencyNodeIndex>()

    private val pendingNodes =
        object : BaseMultimap<IrFileSymbol, DependencyNodeIndex, List<DependencyNodeIndex>, MutableList<DependencyNodeIndex>>() {
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
    private fun StaticInitializationSubgraphBuilder<*, *>.initializeDirectSupertypes(classSymbol: IrClassSymbol) {
        classSymbol.owner.superTypes.forEach { superType ->
            val symbol = superType.classOrNull ?: return@forEach
            // Skip library supertypes, as they cannot have mutual dependencies with the source types, interface types without
            // default methods, and types which are declared outside the current module
            if (symbol !in module || !symbol.isInitializedBySupertypes) return@forEach
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
    private fun IrClassSymbol.collectInitializedDeclarations(): Sequence<IrDeclaration> = sequence {
        // Prevent visiting classes that belong to a library, belong to a different module, are interfaces, or are annotation classes
        if (this@collectInitializedDeclarations !in dependencyGraph.module || owner.kind == ClassKind.INTERFACE || owner.kind == ClassKind.ANNOTATION_CLASS) return@sequence

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
        owner.superClass?.let { yieldAll(it.symbol.collectInitializedDeclarations()) }

        yieldAll(owner.declarations.asSequence().filter {
            it is IrProperty && it.backingField?.initializer != null || it is IrAnonymousInitializer
        })
    }

    private fun DependencyNodeBuilder.postponeClassEntity(classSymbol: IrClassSymbol) {
        val enclosingEntity = when {
            classSymbol.owner.kind.isObject -> classSymbol.asObjectEntity()
            else -> classSymbol.asClassEntity()
        } ?: return
        worklist.add(enclosingEntity.beginInitializationIndex)
    }

    private fun DependencyNodeBuilder.postponeClassEntity(classDeclaration: IrClass) =
        postponeClassEntity(classDeclaration.symbol)

    fun collectDependencies(file: IrFile): List<DependencyNodeIndex> {
        // Skip files outside the current module and already visited files
        if (file !in dependencyGraph.module || !visitedFiles.add(file.symbol)) return pendingNodes.removeKey(file.symbol)

        dependencyGraph.buildGraph(worklist) {
            val callSiteVisitor = CallSiteVisitor(
                module = dependencyGraph.module,
                visitedFiles = visitedFiles,
                graphBuilder = this
            )

            // Collect reachable roots from the given file
            buildFileEntity(file)

            while (worklist.isNotEmpty()) {
                val current = worklist.removeFirst()
                if (processed.add(current)) {
                    when (current) {
                        is TopLevelIndex if visitedFiles.add(current.enclosingEntity.symbol) ->
                            buildFileEntity(current.enclosingEntity.symbol.owner)
                        is ClinitIndex -> {
                            buildClassEntity(current.enclosingEntity)
                            val containingFile = current.containingFile ?: continue
                            pendingNodes.put(containingFile.symbol, current)
                        }
                        is QualifierIndex -> {
                            buildObjectEntity(current.enclosingEntity)
                            val constructor = current.enclosingEntity.symbol.owner.primaryConstructor ?: continue
                            constructor.accept(callSiteVisitor, CallSiteVisitor.CallSiteVisitContext(current, null, true))
                            val containingFile = current.containingFile ?: continue
                            pendingNodes.put(containingFile.symbol, current)
                        }
                        is EnumEntryIndex -> {
                            val constructor = current.enclosingEntity.symbol.owner.correspondingClass?.primaryConstructor ?: continue
                            constructor.accept(
                                callSiteVisitor,
                                CallSiteVisitor.CallSiteVisitContext(current, current.enclosingEntity, true)
                            )
                            val containingFile = current.containingFile ?: continue
                            pendingNodes.put(containingFile.symbol, current)
                        }
                        is PropertyIndex -> {
                            val propertySymbol = current.symbol
                            propertySymbol.owner.accept(
                                callSiteVisitor,
                                CallSiteVisitor.CallSiteVisitContext(current, current.enclosingEntity)
                            )
                            val containingDeclaration = propertySymbol.owner.let {
                                when {
                                    propertySymbol.owner.isTopLevel -> it.fileOrNull?.symbol
                                    else -> it.parentClassOrNull?.symbol
                                }
                            }
                            if (containingDeclaration == current.enclosingEntity?.symbol) {
                                val containingFile = current.containingFile ?: continue
                                pendingNodes.put(containingFile.symbol, current)
                            }
                        }
                        is AnonymousInitializerIndex -> {
                            val initializedSymbol = current.symbol
                            initializedSymbol.owner.accept(
                                callSiteVisitor,
                                CallSiteVisitor.CallSiteVisitContext(current, current.enclosingEntity)
                            )
                            val containingClass = current.symbol.owner.parentClassOrNull?.symbol ?: continue
                            if (containingClass == current.enclosingEntity?.symbol) {
                                val containingFile = current.containingFile ?: continue
                                pendingNodes.put(containingFile.symbol, current)
                            }
                        }
                        is DeclarationIndex<*> -> {
                            current.symbol.owner.accept(callSiteVisitor, CallSiteVisitor.CallSiteVisitContext(current))
                        }
                        is BeginInstanceInitializationIndex -> {
                            current.symbol.buildInitSubgraph {
                                symbol.owner.superClass?.takeIf { it in module }?.symbol?.let { superClass ->
                                    val endNode = superClass.endInitializationIndex
                                    endNode.buildNode()
                                    endNode mustHappenBefore lastConstructedNode
                                    superClass.postponeInitSubgraph()
                                }
                                // Find "relatively" static initialized declarations of the instance class,
                                // i.e., the declarations which have the same value for each instance
                                symbol.owner.declarations.forEach {
                                    when (it) {
                                        is IrProperty if it.backingField?.initializer != null -> PropertyIndex(it.symbol).buildSubgraphNode()
                                        is IrAnonymousInitializer -> AnonymousInitializerIndex(it.symbol).buildSubgraphNode()
                                    }
                                }
                            }
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

    private fun DependencyGraphBuilder.buildFileEntity(file: IrFile) {
        val enclosingEntity = file.symbol.asFileEntity()
        enclosingEntity.buildClinitSubgraph {
            file.declarations.forEach { decl ->
                when (decl) {
                    is IrProperty -> buildProperty(decl)
                    // JVM initialization of the file's class does not initialize the classes declared inside it.
                    // Hence, they will not be connected to the file's happens-before subgraph
                    is IrClass -> postponeClassEntity(decl)
                    else -> {}
                }
            }
        }
    }

    private fun ObjectSubgraphBuilder.buildObjectSubgraphNodes() {
        val objectSymbol = enclosingEntity.symbol
        initializeDirectSupertypes(objectSymbol)
        // Build all initialized declarations (declared or inherited)
        objectSymbol.collectInitializedDeclarations().forEach {
            when (it) {
                is IrProperty -> buildProperty(it)
                is IrAnonymousInitializer -> buildAnonymousInitializer(it)
                else -> {}
            }
        }
        // Find entities declared inside it
        objectSymbol.owner.declarations.forEach { decl ->
            when (decl) {
                is IrClass -> postponeClassEntity(decl)
                else -> {}
            }
        }
    }

    private fun DependencyGraphBuilder.buildObjectEntity(enclosingEntity: EnclosingEntity.Object) = enclosingEntity.buildClinitSubgraph {
        buildObjectSubgraphNodes()
    }

    private fun ClassSubgraphBuilder.buildCompanionObjectEntity(companionObject: EnclosingEntity.Object) =
        companionObject.buildNestedSubgraph {
            buildObjectSubgraphNodes()
        }

    private fun DependencyGraphBuilder.buildClassEntity(enclosingEntity: EnclosingEntity.Class) {
        val classSymbol = enclosingEntity.symbol
        enclosingEntity.buildClinitSubgraph {
            initializeDirectSupertypes(classSymbol)
            // Store the companion object declaration just in case it appears in the declaration list before enum entries, due to serialization
            var companionObjectEntity: EnclosingEntity.Object? = null
            enclosingEntity.symbol.owner.declarations.forEach { decl ->
                when (decl) {
                    is IrProperty if decl.getter?.body is IrSyntheticBody -> buildProperty(decl)
                    is IrEnumEntry -> buildEnumEntryEntity(decl.asEnumEntryEntity())
                    // Only companion objects will connect to this happens-before subgraph
                    is IrClass if decl.kind.isObject && decl.isCompanion ->
                        companionObjectEntity = decl.asObjectEntity()
                    is IrClass -> postponeClassEntity(decl)
                    else -> {}
                }
            }
            // Build the companion object subgraph according to the declaration order of Kotlin's JVM compilation
            companionObjectEntity?.let { buildCompanionObjectEntity(it) }
        }
    }

    private fun ClassSubgraphBuilder.buildEnumEntryEntity(enclosingEntity: EnclosingEntity.EnumEntry) {
        require(enclosingEntity.parentEnclosingEntity == this@buildEnumEntryEntity.enclosingEntity) { "The provided outer entity must match the enum entry's parent!" }
        enclosingEntity.buildNestedSubgraph {
            val symbol = enclosingEntity.symbol.owner.correspondingClass?.symbol ?: enclosingEntity.parentEnclosingEntity.symbol
            symbol.collectInitializedDeclarations().forEach {
                when (it) {
                    is IrProperty -> buildProperty(it)
                    is IrAnonymousInitializer -> buildAnonymousInitializer(it)
                    // NOTE: no classifiers should be accessible from an enum entry's anonymous object, as the enum entry
                    //       has the type of its (parent) enum class
                    else -> {}
                }
            }
        }
    }

    private fun StaticInitializationSubgraphBuilder<*, *>.buildProperty(property: IrProperty) {
        if (!property.isLocal && !property.isVar && property.backingField?.initializer != null) {
            PropertyIndex(property.symbol, enclosingEntity).buildSubgraphNode()
        }
    }

    private fun StaticInitializationSubgraphBuilder<*, *>.buildAnonymousInitializer(initializer: IrAnonymousInitializer) =
        AnonymousInitializerIndex(initializer.symbol, enclosingEntity).buildSubgraphNode()

    fun clear() {
        visitedFiles.clear()
        processed.clear()
    }
}



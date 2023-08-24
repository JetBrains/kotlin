/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirSingleResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeResolverVisitor
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationStatus
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.platformSupertypeUpdater
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*

internal object LLFirSupertypeLazyResolver : LLFirLazyResolver(FirResolvePhase.SUPER_TYPES) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirResolveContextCollector?,
    ) {
        val resolver = LLFirSuperTypeTargetResolver(target, lockProvider, session, scopeSession)
        resolver.resolveDesignation()
    }

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        when (target) {
            is FirClass -> {
                for (superTypeRef in target.superTypeRefs) {
                    checkTypeRefIsResolved(superTypeRef, "class super type", target)
                }
            }

            is FirTypeAlias -> {
                checkTypeRefIsResolved(target.expandedTypeRef, typeRefName = "type alias expanded type", target)
            }
        }
    }
}

private class LLFirSuperTypeTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val supertypeComputationSession: LLFirSupertypeComputationSession = LLFirSupertypeComputationSession(session),
    private val visitedElements: MutableSet<FirElementWithResolveState> = hashSetOf(),
) : LLFirTargetResolver(target, lockProvider, FirResolvePhase.SUPER_TYPES, isJumpingPhase = false) {
    private val supertypeResolver = object : FirSupertypeResolverVisitor(
        session = session,
        supertypeComputationSession = supertypeComputationSession,
        scopeSession = scopeSession,
    ) {
        /**
         * We can do nothing here because at a call moment we've already resolved [outerClass]
         * because we resolve classes from top to down
         */
        override fun resolveAllSupertypesForOuterClass(outerClass: FirClass) {
            // We can get into this function during a loop calculation, so it is possible that the result for [outerClass]
            // is not yet published, so we expect that this class was already visited or resolved
            if (outerClass !in visitedElements) {
                outerClass.asResolveTarget()?.let { resolveTarget ->
                    // It is possible in case of declaration collision,
                    // so we need this logic only to be sure that [outerClass] is resolved
                    resolveToSupertypePhase(resolveTarget)
                }

                LLFirSupertypeLazyResolver.checkIsResolved(outerClass)
            }
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        supertypeResolver.withClass(firClass) {
            doResolveWithoutLock(firClass)
            action()
        }
    }

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        val isVisited = !visitedElements.add(target)
        if (isVisited) return true

        when (target) {
            is FirRegularClass -> performResolve(
                declaration = target,
                superTypeRefsForTransformation = {
                    // We should create a copy of the original collection
                    // to avoid [ConcurrentModificationException] during another thread publication
                    ArrayList(target.superTypeRefs)
                },
                resolver = { supertypeResolver.resolveSpecificClassLikeSupertypes(target, it) },
                superTypeUpdater = {
                    target.replaceSuperTypeRefs(it)
                    session.platformSupertypeUpdater?.updateSupertypesIfNeeded(target, scopeSession)
                },
            )
            is FirTypeAlias -> performResolve(
                declaration = target,
                superTypeRefsForTransformation = { target.expandedTypeRef },
                resolver = { supertypeResolver.resolveTypeAliasSupertype(target, it, resolveRecursively = false) },
                superTypeUpdater = { target.replaceExpandedTypeRef(it.single()) },
            )
            else -> {
                resolveFileAnnotationContainerIfNeeded(target)
                performCustomResolveUnderLock(target) {
                    // just update the phase
                }
            }
        }

        return true
    }

    /**
     * [superTypeRefsForTransformation] will be executed under [declaration] lock
     */
    private inline fun <T : FirClassLikeDeclaration, S> performResolve(
        declaration: T,
        superTypeRefsForTransformation: () -> S,
        resolver: (S) -> List<FirResolvedTypeRef>,
        crossinline superTypeUpdater: (List<FirTypeRef>) -> Unit,
    ) {
        // To avoid redundant work, because a publication won't be executed
        if (declaration.resolvePhase >= resolverPhase) return

        declaration.lazyResolveToPhase(resolverPhase.previous)

        var superTypeRefs: S? = null
        withReadLock(declaration) {
            superTypeRefs = superTypeRefsForTransformation()
        }

        // "null" means that some other thread is already resolved [declaration] to [resolverPhase]
        if (superTypeRefs == null) return

        // 1. Resolve declaration super type refs
        @Suppress("UNCHECKED_CAST")
        val resolvedSuperTypeRefs = resolver(superTypeRefs as S)

        // 2. Resolve super declarations
        val status = supertypeComputationSession.getSupertypesComputationStatus(declaration)
        if (status is SupertypeComputationStatus.Computed) {
            for (computedType in status.supertypeRefs) {
                crawlSupertype(computedType.type)
            }
        }

        // 3. Find loops
        val loopedSuperTypeRefs = supertypeComputationSession.findLoopFor(declaration)

        // 4. Get error type refs or already resolved
        val resultedTypeRefs = loopedSuperTypeRefs ?: resolvedSuperTypeRefs

        // 5. Publish the result
        performCustomResolveUnderLock(declaration) {
            superTypeUpdater(resultedTypeRefs)
        }
    }

    private fun FirClassLikeDeclaration.asResolveTarget(): LLFirSingleResolveTarget? {
        return takeIf { it.canHaveLoopInSupertypesHierarchy(session) }
            ?.tryCollectDesignationWithFile()
            ?.asResolveTarget()
    }

    private fun resolveToSupertypePhase(target: LLFirSingleResolveTarget) {
        LLFirSuperTypeTargetResolver(
            target = target,
            lockProvider = lockProvider,
            session = session,
            scopeSession = scopeSession,
            supertypeComputationSession = supertypeComputationSession,
            visitedElements = visitedElements,
        ).resolveDesignation()

        LLFirLazyPhaseResolverByPhase.getByPhase(resolverPhase).checkIsResolved(target)
    }

    /**
     * We want to apply resolved supertypes to as many designations as possible.
     * So we crawl the resolved supertypes of visited designations to find more designations to collect.
     */
    private fun crawlSupertype(type: ConeKotlinType) {
        val classLikeDeclaration = type.toSymbol(session)?.fir
        if (classLikeDeclaration !is FirClassLikeDeclaration) return
        if (classLikeDeclaration in visitedElements) return
        if (classLikeDeclaration is FirJavaClass) {
            if (!classLikeDeclaration.canHaveLoopInSupertypesHierarchy(session)) return

            visitedElements += classLikeDeclaration
            val parentClass = classLikeDeclaration.outerClass(session)
            if (parentClass != null) {
                crawlSupertype(parentClass.defaultType())
            }

            classLikeDeclaration.superTypeRefs.filterIsInstance<FirResolvedTypeRef>().forEach {
                crawlSupertype(it.type)
            }

            return
        }

        val resolveTarget = classLikeDeclaration.asResolveTarget()
        if (resolveTarget != null) {
            resolveToSupertypePhase(resolveTarget)
        } else if (type is ConeClassLikeType) {
            // The `classLikeDeclaration` is not associated with a file, and thus there is no need to resolve it, but it may still point
            // to declarations via its type arguments which need to be collected and have a containing file.
            // For example, a `Function1` could point to a type alias.
            type.typeArguments.forEach { it.type?.let(::crawlSupertype) }
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        error("Should be resolved without lock in ${::doResolveWithoutLock.name}")
    }
}

/**
 * We shouldn't skip Java source classes because they're marked as BODY_RESOLVE,
 * but this doesn't give us knowledge about its participation in the calculation of supertypes.
 * The contract here – if a declaration is already resolved to FirResolvePhase.SUPER_TYPES or higher that this
 * means that this class can't have loop with our class, because in this case this declaration will be present
 * in the current supertypes resolve session
 */
private fun FirClassLikeDeclaration.canHaveLoopInSupertypesHierarchy(session: FirSession): Boolean = when {
    this is FirJavaClass -> origin is FirDeclarationOrigin.Java.Source
    origin !is FirDeclarationOrigin.Source -> false
    resolvePhase < FirResolvePhase.SUPER_TYPES -> true
    // We should still process resolved if it has loop in super type refs, because we can be part of this cycle
    this is FirRegularClass && superTypeRefs.any(FirTypeRef::isLoopedSupertypeRef) -> true
    this is FirTypeAlias && expandedTypeRef.isLoopedSupertypeRef -> true
    else -> outerClass(session)?.canHaveLoopInSupertypesHierarchy(session) == true
}

private fun FirClassLikeDeclaration.outerClass(session: FirSession): FirRegularClass? = symbol.classId.parentClassId?.let { parentClassId ->
    session.symbolProvider.getClassLikeSymbolByClassId(parentClassId)?.fir as? FirRegularClass
}

private val FirTypeRef.isLoopedSupertypeRef: Boolean
    get() {
        if (this !is FirErrorTypeRef) return false
        val diagnostic = diagnostic
        return diagnostic is ConeSimpleDiagnostic && diagnostic.kind == DiagnosticKind.LoopInSupertype
    }

private class LLFirSupertypeComputationSession(private val session: FirSession) : SupertypeComputationSession() {
    /**
     * These collections exist to reuse a collection for each search to avoid repeated memory allocation.
     * Can be replaced with a new collection on each invocation of [findLoopFor]
     */
    private val visited: MutableSet<FirClassLikeDeclaration> = hashSetOf()
    private val looped: MutableSet<FirClassLikeDeclaration> = hashSetOf()
    private val pathSet: MutableSet<FirClassLikeDeclaration> = hashSetOf()
    private val path: MutableList<FirClassLikeDeclaration> = mutableListOf()
    // ---------------

    /**
     * Map from [FirClassLikeDeclaration] to [List<FirResolvedTypeRef>>],
     * where the list contains at least one [FirErrorTypeRef] for looped references
     */
    private val updatedTypesForDeclarationsWithLoop: MutableMap<FirClassLikeDeclaration, List<FirResolvedTypeRef>> = hashMapOf()

    /**
     * @param declaration declaration to be checked for loops
     * @return list of resolved super type refs if at least one of them is [FirErrorTypeRef] due to cycle hierarchy
     */
    fun findLoopFor(declaration: FirClassLikeDeclaration): List<FirResolvedTypeRef>? {
        breakLoopFor(
            declaration = declaration,
            session = session,
            visited = visited,
            looped = looped,
            pathSet = pathSet,
            path = path,
        )

        require(path.isEmpty()) { "Path should be empty" }
        require(pathSet.isEmpty()) { "Path set should be empty" }
        visited.clear()
        looped.clear()
        return updatedTypesForDeclarationsWithLoop[declaration]
    }

    override fun isAlreadyResolved(classLikeDeclaration: FirClassLikeDeclaration): Boolean {
        return !classLikeDeclaration.canHaveLoopInSupertypesHierarchy(session)
    }

    /**
     * We shouldn't try to iterate over unresolved class. Otherwise, it can lead to [ConcurrentModificationException]
     */
    override fun getResolvedSuperTypeRefsForOutOfSessionDeclaration(classLikeDeclaration: FirClassLikeDeclaration): List<FirResolvedTypeRef>? {
        if (classLikeDeclaration.resolvePhase < FirResolvePhase.SUPER_TYPES) return emptyList()

        return super.getResolvedSuperTypeRefsForOutOfSessionDeclaration(classLikeDeclaration)
    }

    /**
     * It is possible that one of super type refs were already reported as an error, but the second – not.
     * So in this case, we want to save already reported errors and add a new one.
     * Example:
     * ```
     * interface B : A, ResolveMe {}
     * interface C : B {}
     * interface D : B {}
     * interface ResolveMe<caret> : F {}
     * // D will be marked as error during ResolveMe->F->D->B->ResolveMe round.
     * // And we will back to super type refs of class F during the resolution of class C
     * interface F : D, C {}
     * interface NonLoopedInterface : C
     * ```
     */
    override fun reportLoopErrorRefs(classLikeDeclaration: FirClassLikeDeclaration, supertypeRefs: List<FirResolvedTypeRef>) {
        updatedTypesForDeclarationsWithLoop.merge(classLikeDeclaration, supertypeRefs) { oldRefs, newRefs ->
            buildList<FirResolvedTypeRef>(oldRefs.size) {
                for ((old, new) in oldRefs.zip(newRefs)) {
                    if (old is FirErrorTypeRef) {
                        add(old)
                    } else {
                        add(new)
                    }
                }
            }
        }
    }
}

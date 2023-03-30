/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withInvalidationOnException
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirApplySupertypesTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeResolverVisitor
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationStatus
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal object LLFirSupertypeLazyResolver : LLFirLazyResolver(FirResolvePhase.SUPER_TYPES) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val resolver = LLFirSuperTypeTargetResolver(target, lockProvider, session, scopeSession)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(resolverPhase)
        when (target) {
            is FirClass -> {
                for (superTypeRef in target.superTypeRefs) {
                    checkTypeRefIsResolved(superTypeRef, "class super type", target)
                }
            }

            is FirTypeAlias -> {
                checkTypeRefIsResolved(target.expandedTypeRef, typeRefName = "type alias expanded type", target)
            }

            else -> {}
        }
    }
}

internal class LLFirSuperTypeTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
) : LLFirTargetResolver(target, lockProvider, FirResolvePhase.SUPER_TYPES) {

    private val supertypeComputationSession = SupertypeComputationSession()

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        doResolveWithoutLock(firClass)
        action()
    }

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        when (target) {
            is FirRegularClass, is FirTypeAlias -> {
                val designationToResolve = FirDesignationWithFile(nestedClassesStack, target, resolveTarget.firFile)
                val collected = collect(designationToResolve)
                supertypeComputationSession.breakLoops(session)
                apply(collected)
            }
            else -> {
                performCustomResolveUnderLock(target) {
                    // just update the phase
                }
            }
        }
        return true
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        error("Should be resolved without lock in ${::doResolveWithoutLock.name}")
    }

    private inner class DesignatedFirSupertypeResolverVisitor(classDesignation: FirDesignation) :
        FirSupertypeResolverVisitor(
            session = session,
            supertypeComputationSession = supertypeComputationSession,
            scopeSession = scopeSession,
            scopeForLocalClass = null,
            localClassesNavigationInfo = null,
        ) {
        val declarationTransformer = LLFirDeclarationTransformer(classDesignation)

        override fun visitDeclarationContent(declaration: FirDeclaration, data: Any?) {
            declarationTransformer.visitDeclarationContent(this, declaration, data) {
                super.visitDeclarationContent(declaration, data)
                declaration
            }
        }
    }

    private inner class DesignatedFirApplySupertypesTransformer(classDesignation: FirDesignation) :
        FirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession) {

        val declarationTransformer = LLFirDeclarationTransformer(classDesignation)

        override fun transformDeclarationContent(declaration: FirDeclaration, data: Any?): FirDeclaration {
            return declarationTransformer.transformDeclarationContent(this, declaration, data) {
                super.transformDeclarationContent(declaration, data)
            }
        }
    }

    private inner class DesignationCollector {
        private val visited = mutableMapOf<FirElementWithResolveState, FirDesignationWithFile>()
        private val toVisit = mutableListOf<FirDesignationWithFile>()

        fun collect(designation: FirDesignationWithFile): Collection<FirDesignationWithFile> {
            toVisit.add(designation)
            while (toVisit.isNotEmpty()) {
                toVisit.forEach(::visitDesignation)
                toVisit.clear()

                // We want to apply resolved supertypes to as many designations as possible. So we crawl the resolved supertypes of visited
                // designations to find more designations to collect.
                for (supertypeComputationStatus in supertypeComputationSession.supertypeStatusMap.values) {
                    if (supertypeComputationStatus !is SupertypeComputationStatus.Computed) continue
                    supertypeComputationStatus.supertypeRefs.forEach { crawlSupertype(it.type) }
                }
            }

            return visited.values
        }

        private fun visitDesignation(designation: FirDesignationWithFile) {
            checkCanceled()
            val resolver = DesignatedFirSupertypeResolverVisitor(designation)
            designation.firFile.lazyResolveToPhase(FirResolvePhase.IMPORTS)
            designation.firFile.accept(resolver, null)
            resolver.declarationTransformer.ensureDesignationPassed()
            visited[designation.target] = designation
        }

        private fun crawlSupertype(type: ConeKotlinType) {
            val classLikeDeclaration = type.toSymbol(session)?.fir
            if (classLikeDeclaration !is FirClassLikeDeclaration) return
            if (classLikeDeclaration is FirJavaClass) return
            if (visited.containsKey(classLikeDeclaration)) return

            val containingFile =
                classLikeDeclaration.llFirResolvableSession?.moduleComponents?.cache?.getContainerFirFile(classLikeDeclaration)
            if (containingFile != null) {
                toVisit.add(classLikeDeclaration.collectDesignation(containingFile))
            } else if (type is ConeClassLikeType) {
                // The `classLikeDeclaration` is not associated with a file and thus there is no need to resolve it, but it may still point
                // to declarations via its type arguments which need to be collected and have a containing file. For example, a `Function1`
                // could point to a type alias.
                type.typeArguments.forEach { it.type?.let(::crawlSupertype) }
            }
        }
    }

    /**
     * Resolves the supertypes of [designation] and collects all designations that resolved supertypes should be applied to, including
     * [designation] itself and any designations discovered via resolved supertypes.
     */
    private fun collect(designation: FirDesignationWithFile): Collection<FirDesignationWithFile> =
        DesignationCollector().collect(designation)

    /**
     * Applies the resolved supertypes in [supertypeComputationSession] to each designation in [designations].
     */
    private fun apply(designations: Collection<FirDesignationWithFile>) {
        fun applyToFileSymbols(designations: List<FirDesignationWithFile>) {
            for (designation in designations) {
                checkCanceled()
                val applier = DesignatedFirApplySupertypesTransformer(designation)
                (designation.target as FirDeclaration).lazyResolveToPhase(resolverPhase.previous)
                performCustomResolveUnderLock(designation.target) {
                    designation.firFile.transform<FirElement, Void?>(applier, null)
                    applier.declarationTransformer.ensureDesignationPassed()
                }
            }
        }

        val filesToDesignations = designations.groupBy { it.firFile }
        for (designationsPerFile in filesToDesignations) {
            checkCanceled()
            val session = designationsPerFile.key.llFirResolvableSession
                ?: error("When FirFile exists for the declaration, the session should be resolvable")
            withInvalidationOnException(session) {
                applyToFileSymbols(designationsPerFile.value)
            }
        }
    }
}

private class LLFirDeclarationTransformer(private val designation: FirDesignation) {
    private val designationWithoutTargetIterator = designation.toSequence(includeTarget = false).iterator()
    private var isInsideTargetDeclaration: Boolean = false
    private var designationPassed: Boolean = false

    inline fun <D> visitDeclarationContent(
        visitor: FirVisitor<Unit, D>,
        declaration: FirDeclaration,
        data: D,
        default: () -> FirDeclaration
    ): FirDeclaration = processDeclarationContent(declaration, default) {
        it.accept(visitor, data)
    }

    inline fun <D> transformDeclarationContent(
        transformer: FirDefaultTransformer<D>,
        declaration: FirDeclaration,
        data: D,
        default: () -> FirDeclaration
    ): FirDeclaration = processDeclarationContent(declaration, default) { toTransform ->
        toTransform.transform<FirElement, D>(transformer, data).also { transformed ->
            check(transformed === toTransform) {
                "become $transformed `${transformed.render()}`, was ${toTransform}: `${toTransform.render()}`"
            }
        }
    }

    private inline fun processDeclarationContent(
        declaration: FirDeclaration,
        default: () -> FirDeclaration,
        applyToDesignated: (FirElementWithResolveState) -> Unit,
    ): FirDeclaration {
        //It means that we are inside the target declaration
        if (isInsideTargetDeclaration) {
            return default()
        }

        //It means that we already transform target declaration and now can skip all others
        if (designationPassed) {
            return declaration
        }

        if (designationWithoutTargetIterator.hasNext()) {
            applyToDesignated(designationWithoutTargetIterator.next())
        } else {
            try {
                isInsideTargetDeclaration = true
                designationPassed = true
                applyToDesignated(designation.target)
            } finally {
                isInsideTargetDeclaration = false
            }
        }

        return declaration
    }

    fun ensureDesignationPassed() {
        check(designationPassed) { "Designation not passed for declaration ${designation.target::class.simpleName}" }
    }
}

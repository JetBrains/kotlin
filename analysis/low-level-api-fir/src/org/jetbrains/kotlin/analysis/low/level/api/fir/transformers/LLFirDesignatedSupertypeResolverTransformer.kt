/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withInvalidationOnException
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.types.type

/**
 * Transform designation into SUPER_TYPES phase. Affects only the designation, target declaration, its children, and dependents.
 */
internal class LLFirDesignatedSupertypeResolverTransformer(
    private val designation: FirDesignationWithFile,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val lockProvider: LLFirLockProvider
) : LLFirLazyTransformer {

    private val supertypeComputationSession = SupertypeComputationSession()

    private inner class DesignatedFirSupertypeResolverVisitor(classDesignation: FirDesignation) :
        FirSupertypeResolverVisitor(
            session = session,
            supertypeComputationSession = supertypeComputationSession,
            scopeSession = scopeSession,
            scopeForLocalClass = null,
            localClassesNavigationInfo = null
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
            lockProvider.withLock(designation.firFile) {
                designation.firFile.accept(resolver, null)
            }
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
                designation.firFile.transform<FirElement, Void?>(applier, null)
                applier.declarationTransformer.ensureDesignationPassed()
            }
        }

        val filesToDesignations = designations.groupBy { it.firFile }
        for (designationsPerFile in filesToDesignations) {
            checkCanceled()
            lockProvider.withLock(designationsPerFile.key) {
                val session = designationsPerFile.key.llFirResolvableSession
                    ?: error("When FirFile exists for the declaration, the session should be resolvevablable")
                withInvalidationOnException(session) {
                    applyToFileSymbols(designationsPerFile.value)
                }
            }
        }
    }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.target.resolvePhase >= FirResolvePhase.SUPER_TYPES) return
        designation.firFile.checkPhase(FirResolvePhase.IMPORTS)

        val targetDesignation = if (designation.target !is FirClassLikeDeclaration) {
            val resolvableTarget = designation.path.lastOrNull()
            if (resolvableTarget == null) {
                updatePhaseDeep(designation.target, FirResolvePhase.SUPER_TYPES)
                return
            }
            val targetPath = designation.path.dropLast(1)
            FirDesignationWithFile(targetPath, resolvableTarget, designation.firFile)
        } else designation

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.SUPER_TYPES) {
            val collected = collect(targetDesignation)
            supertypeComputationSession.breakLoops(session)
            apply(collected)
        }

        updatePhaseDeep(designation.target, FirResolvePhase.SUPER_TYPES)
        checkIsResolved(designation.target)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.SUPER_TYPES)
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
        checkNestedDeclarationsAreResolved(target)
    }
}

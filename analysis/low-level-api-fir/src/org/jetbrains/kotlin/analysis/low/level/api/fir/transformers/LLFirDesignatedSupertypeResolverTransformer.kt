/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolvePhase
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.toSymbol

/**
 * Transform designation into SUPER_TYPES phase. Affects only for designation, target declaration, it's children and dependents
 */
internal class LLFirDesignatedSupertypeResolverTransformer(
    private val designation: FirDesignationWithFile,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val lockProvider: LLFirLockProvider,
    private val firProviderInterceptor: FirProviderInterceptor?,
    private val checkPCE: Boolean,
) : LLFirLazyTransformer {

    private val supertypeComputationSession = SupertypeComputationSession()

    private inner class DesignatedFirSupertypeResolverVisitor(classDesignation: FirDesignation) :
        FirSupertypeResolverVisitor(
            session = session,
            supertypeComputationSession = supertypeComputationSession,
            scopeSession = scopeSession,
            scopeForLocalClass = null,
            localClassesNavigationInfo = null,
            firProviderInterceptor = firProviderInterceptor,
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

    private fun collect(designation: FirDesignationWithFile): Collection<FirDesignationWithFile> {
        val visited = mutableMapOf<FirElementWithResolvePhase, FirDesignationWithFile>()
        val toVisit = mutableListOf<FirDesignationWithFile>()

        toVisit.add(designation)
        while (toVisit.isNotEmpty()) {
            for (nowVisit in toVisit) {
                if (checkPCE) checkCanceled()
                val resolver = DesignatedFirSupertypeResolverVisitor(nowVisit)
                nowVisit.firFile.lazyResolveToPhase(FirResolvePhase.IMPORTS)
                lockProvider.runCustomResolveUnderLock(nowVisit.firFile, checkPCE) {
                    nowVisit.firFile.accept(resolver, null)
                }
                resolver.declarationTransformer.ensureDesignationPassed()
                visited[nowVisit.target] = nowVisit
            }
            toVisit.clear()

            for (value in supertypeComputationSession.supertypeStatusMap.values) {
                if (value !is SupertypeComputationStatus.Computed) continue
                for (reference in value.supertypeRefs) {
                    val classLikeDeclaration = reference.type.toSymbol(session)?.fir
                    if (classLikeDeclaration !is FirClassLikeDeclaration) continue
                    if (classLikeDeclaration is FirJavaClass) continue
                    if (visited.containsKey(classLikeDeclaration)) continue
                    val cache = classLikeDeclaration.llFirResolvableSession?.moduleComponents?.cache ?: continue
                    val containingFile = cache.getContainerFirFile(classLikeDeclaration) ?: continue
                    toVisit.add(classLikeDeclaration.collectDesignation(containingFile))
                }
            }
        }
        return visited.values
    }

    private fun apply(visited: Collection<FirDesignationWithFile>) {
        fun applyToFileSymbols(designations: List<FirDesignationWithFile>) {
            for (designation in designations) {
                if (checkPCE) checkCanceled()
                val applier = DesignatedFirApplySupertypesTransformer(designation)
                designation.firFile.transform<FirElement, Void?>(applier, null)
                applier.declarationTransformer.ensureDesignationPassed()
            }
        }

        val filesToDesignations = visited.groupBy { it.firFile }
        for (designationsPerFile in filesToDesignations) {
            if (checkPCE) checkCanceled()
            lockProvider.runCustomResolveUnderLock(designationsPerFile.key, checkPCE) {
                val session = designationsPerFile.key.llFirResolvableSession
                    ?: error("When FirFile exists for the declaration, the session should be resolvevablable")
                session.moduleComponents.sessionInvalidator.withInvalidationOnException(session) {
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
            check(resolvableTarget is FirClassLikeDeclaration)
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

    override fun checkIsResolved(target: FirElementWithResolvePhase) {
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

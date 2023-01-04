/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.toSymbol

internal object LLFirDesignatedSupertypeResolverPhaseResolver : LLFirLazyPhaseResolver() {

    override fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        designation.path.forEach {
            resolveSupertypesForClass(
                it.collectDesignationWithFile(),
                lockProvider,
                session,
                scopeSession,
                firProviderInterceptor,
                false
            )
        }
        when (designation) {
            is LLFirDesignationForResolveWithMembers -> {
                resolveSupertypesForClass(
                    designation.asDesignationForTarget(),
                    lockProvider,
                    session,
                    scopeSession,
                    firProviderInterceptor,
                    resolveMembersInsideTarget = false
                )
            }
            is LLFirDesignationForResolveWithMultipleTargets -> {
                for (targetDesignation in designation.asDesignationSequence()) {
                    resolveSupertypesForClass(
                        targetDesignation,
                        lockProvider,
                        session,
                        scopeSession,
                        firProviderInterceptor, designation.resolveMembersInsideTarget
                    )
                }
            }
        }

    }

    private class DesignatedFirSupertypeResolverVisitor(
        classDesignation: FirDesignationWithFile,
        session: FirSession,
        scopeSession: ScopeSession,
        firProviderInterceptor: FirProviderInterceptor?,
        supertypeComputationSession: SupertypeComputationSession,
    ) : FirSupertypeResolverVisitor(
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

    private class DesignatedFirApplySupertypesTransformer(
        designation: FirDesignationWithFile,
        session: FirSession,
        scopeSession: ScopeSession,
        supertypeComputationSession: SupertypeComputationSession,
    ) :
        FirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession) {

        val declarationTransformer = LLFirDeclarationTransformer(designation)

        override fun transformDeclarationContent(declaration: FirDeclaration, data: Any?): FirDeclaration {
            return declarationTransformer.transformDeclarationContent(this, declaration, data) {
                super.transformDeclarationContent(declaration, data)
            }
        }
    }

    private fun collect(
        designation: FirDesignationWithFile,
        session: FirSession,
        scopeSession: ScopeSession,
        firProviderInterceptor: FirProviderInterceptor?,
        supertypeComputationSession: SupertypeComputationSession,
    ): Collection<FirDesignationWithFile> {
        val visited = mutableMapOf<FirElementWithResolveState, FirDesignationWithFile>()
        val toVisit = mutableListOf<FirDesignationWithFile>()

        toVisit.add(designation)
        while (toVisit.isNotEmpty()) {
            for (nowVisit in toVisit) {
                checkCanceled()
                val resolver = DesignatedFirSupertypeResolverVisitor(
                    nowVisit,
                    session,
                    scopeSession,
                    firProviderInterceptor,
                    supertypeComputationSession
                )
                nowVisit.firFile.lazyResolveToPhase(FirResolvePhase.IMPORTS)
                nowVisit.firFile.accept(resolver, null)
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

    private fun apply(
        visited: Collection<FirDesignationWithFile>,
        lockProvider: LLFirLockProvider,
        scopeSession: ScopeSession,
        supertypeComputationSession: SupertypeComputationSession,
    ) {
        fun applyToFileSymbols(designations: List<FirDesignationWithFile>, session: FirSession) {
            for (designation in designations) {
                checkCanceled()
                lockProvider.withLock(designation.target, FirResolvePhase.SUPER_TYPES) {
                    val applier = DesignatedFirApplySupertypesTransformer(
                        designation,
                        session,
                        scopeSession,
                        supertypeComputationSession
                    )
                    designation.firFile.transform<FirElement, Void?>(applier, null)
                    updatePhaseForDeclarationInternals(designation.target)
                }
            }
        }

        val filesToDesignations = visited.groupBy { it.firFile }
        for (designationsPerFile in filesToDesignations) {
            checkCanceled()
            val session = designationsPerFile.key.llFirResolvableSession
                ?: error("When FirFile exists for the declaration, the session should be resolvevablable")
            session.moduleComponents.sessionInvalidator.withInvalidationOnException(session) {
                applyToFileSymbols(designationsPerFile.value, session)
            }
        }
    }


    private fun resolveSupertypesForClass(
        targetDesignation: FirDesignationWithFile,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        firProviderInterceptor: FirProviderInterceptor?,
        resolveMembersInsideTarget: Boolean,
    ) {
        val supertypeComputationSession = SupertypeComputationSession()
        val collected = collect(
            targetDesignation,
            session,
            scopeSession,
            firProviderInterceptor,
            supertypeComputationSession,
        )
        supertypeComputationSession.breakLoops(session)
        apply(
            collected,
            lockProvider,
            scopeSession,
            supertypeComputationSession,
        )

        if (resolveMembersInsideTarget) {
            (targetDesignation.target as? FirRegularClass)?.let { firClass ->
                for (nestedClass in firClass.declarations) {
                    resolveSupertypesForClass(
                        targetDesignation.nestedDesignation(nestedClass),
                        lockProvider,
                        session,
                        scopeSession,
                        firProviderInterceptor,
                        resolveMembersInsideTarget = true
                    )
                }
            }
        }
    }


    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, FirResolvePhase.SUPER_TYPES, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        when (target) {
            is FirClass -> {
                target.checkPhase(FirResolvePhase.SUPER_TYPES)
                for (superTypeRef in target.superTypeRefs) {
                    checkTypeRefIsResolved(superTypeRef, "class super type", target)
                }
            }

            is FirTypeAlias -> {
                target.checkPhase(FirResolvePhase.SUPER_TYPES)
                checkTypeRefIsResolved(target.expandedTypeRef, typeRefName = "type alias expanded type", target)
            }

            else -> {}
        }
    }
}
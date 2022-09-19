/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirModuleLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.types.toSymbol

/**
 * Transform designation into SUPER_TYPES phase. Affects only for designation, target declaration, it's children and dependents
 */
internal class LLFirDesignatedSupertypeResolverTransformer(
    private val designation: FirDeclarationDesignationWithFile,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val firLazyDeclarationResolver: LLFirModuleLazyDeclarationResolver,
    private val lockProvider: LLFirLockProvider,
    private val firProviderInterceptor: FirProviderInterceptor?,
    private val checkPCE: Boolean,
) : LLFirLazyTransformer {

    private val supertypeComputationSession = SupertypeComputationSession()

    private inner class DesignatedFirSupertypeResolverVisitor(classDesignation: FirDeclarationDesignation) :
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

    private inner class DesignatedFirApplySupertypesTransformer(classDesignation: FirDeclarationDesignation) :
        FirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession) {

        val declarationTransformer = LLFirDeclarationTransformer(classDesignation)

        override fun transformDeclarationContent(declaration: FirDeclaration, data: Any?): FirDeclaration {
            return declarationTransformer.transformDeclarationContent(this, declaration, data) {
                super.transformDeclarationContent(declaration, data)
            }
        }
    }

    private fun collect(designation: FirDeclarationDesignationWithFile): Collection<FirDeclarationDesignationWithFile> {
        val visited = mutableMapOf<FirDeclaration, FirDeclarationDesignationWithFile>()
        val toVisit = mutableListOf<FirDeclarationDesignationWithFile>()

        toVisit.add(designation)
        while (toVisit.isNotEmpty()) {
            for (nowVisit in toVisit) {
                if (checkPCE) checkCanceled()
                val resolver = DesignatedFirSupertypeResolverVisitor(nowVisit)
                lockProvider.runCustomResolveUnderLock(nowVisit.firFile, checkPCE) {
                    firLazyDeclarationResolver.lazyResolveFileDeclaration(
                        firFile = nowVisit.firFile,
                        toPhase = FirResolvePhase.IMPORTS,
                        scopeSession = scopeSession,
                        checkPCE = true,
                    )
                    nowVisit.firFile.accept(resolver, null)
                }
                resolver.declarationTransformer.ensureDesignationPassed()
                visited[nowVisit.declaration] = nowVisit
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

    private fun apply(visited: Collection<FirDeclarationDesignationWithFile>) {
        fun applyToFileSymbols(designations: List<FirDeclarationDesignationWithFile>) {
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
                applyToFileSymbols(designationsPerFile.value)
            }
        }
    }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.SUPER_TYPES) return
        designation.firFile.checkPhase(FirResolvePhase.IMPORTS)

        val targetDesignation = if (designation.declaration !is FirClassLikeDeclaration) {
            val resolvableTarget = designation.path.lastOrNull()
            if (resolvableTarget == null) {
                updatePhaseDeep(designation.declaration, FirResolvePhase.SUPER_TYPES)
                return
            }
            check(resolvableTarget is FirClassLikeDeclaration)
            val targetPath = designation.path.dropLast(1)
            FirDeclarationDesignationWithFile(targetPath, resolvableTarget, designation.firFile)
        } else designation

        ResolveTreeBuilder.resolvePhase(designation.declaration, FirResolvePhase.SUPER_TYPES) {
            phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.SUPER_TYPES) {
                val collected = collect(targetDesignation)
                supertypeComputationSession.breakLoops(session)
                apply(collected)
            }
        }

        updatePhaseDeep(designation.declaration, FirResolvePhase.SUPER_TYPES)
        checkIsResolved(designation.declaration)
    }

    override fun checkIsResolved(declaration: FirDeclaration) {
        declaration.checkPhase(FirResolvePhase.SUPER_TYPES)
        when (declaration) {
            is FirClass -> {
                for (superTypeRef in declaration.superTypeRefs) {
                    checkTypeRefIsResolved(superTypeRef, "class super type", declaration)
                }
            }

            is FirTypeAlias -> {
                checkTypeRefIsResolved(declaration.expandedTypeRef, typeRefName = "type alias expanded type", declaration)
            }

            else -> {}
        }
        checkNestedDeclarationsAreResolved(declaration)
    }
}

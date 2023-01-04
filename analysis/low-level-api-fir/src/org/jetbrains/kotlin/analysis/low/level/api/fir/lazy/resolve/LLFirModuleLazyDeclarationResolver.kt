/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidator
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirFirProviderInterceptor
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformerExecutor
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.rethrowExceptionWithDetails
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

internal class LLFirModuleLazyDeclarationResolver(val moduleComponents: LLFirModuleResolveComponents) {



    private fun resolveContainingFileToImports(target: FirElementWithResolveState) {
        if (target.resolveState.resolvePhase >= FirResolvePhase.IMPORTS) return
        val firFile = target.getContainingFile() ?: return
        if (firFile.resolveState.resolvePhase >= FirResolvePhase.IMPORTS) return
        moduleComponents.globalResolveComponents.lockProvider.withLock(firFile, FirResolvePhase.IMPORTS) {
            resolveFileToImportsWithoutLock(firFile)
        }
    }

    private fun resolveFileToImportsWithoutLock(firFile: FirFile) {
        if (firFile.resolveState.resolvePhase >= FirResolvePhase.IMPORTS) return
        checkCanceled()
        firFile.transform<FirElement, Any?>(FirImportResolveTransformer(firFile.moduleData.session), null)
    }

    /**
     * Run designated resolve only designation with fully resolved path (synchronized).
     * Suitable for body resolve or/and on-air resolve.
     * @see lazyResolve for ordinary resolve
     * @param target target non-local declaration
     */
    fun lazyResolve(
        target: FirElementWithResolveState,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
    ) {
        val fromPhase = target.resolveState.resolvePhase
        try {
            doLazyResolve(target, scopeSession, toPhase)
        } catch (e: Exception) {
            handleExceptionFromResolve(e, moduleComponents.sessionInvalidator, target, fromPhase, toPhase)
        }
    }

    private fun doLazyResolve(
        target: FirElementWithResolveState,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
    ) {
        if (target.resolveState.resolvePhase >= toPhase) return
        resolveContainingFileToImports(target)
        if (toPhase == FirResolvePhase.IMPORTS) return

        lazyResolveDesignations(
            designations = LLFirResolveMultiDesignationCollector.getDesignationsToResolve(target),
            scopeSession = scopeSession,
            toPhase = toPhase,
        )
    }



    private fun lazyResolveDesignations(
        designations: List<LLFirDesignationToResolve>,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
    ) {
        if (designations.isEmpty()) return

        var currentPhase = run {
            var min = FirResolvePhase.BODY_RESOLVE
            for (designation in designations) {
                when (designation) {
                    is LLFirDesignationForResolveWithMembers -> {
                        min = minOf(min, designation.target.resolveState.resolvePhase)
                        for (target in designation.callableMembersToResolve) {
                            min = minOf(min, target.resolveState.resolvePhase)
                        }
                    }
                    is LLFirDesignationForResolveWithMultipleTargets -> {
                        for (target in designation.targets) {
                            min = minOf(min, target.resolveState.resolvePhase)
                        }
                    }
                }

                for (pathPart in designation.path) {
                    min = minOf(min, pathPart.resolveState.resolvePhase)
                }
            }
            min
        }.coerceAtLeast(FirResolvePhase.IMPORTS)

        if (currentPhase >= toPhase) return

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            checkCanceled()

            for (designation in designations) {
                LLFirLazyTransformerExecutor.execute(
                    phase = currentPhase,
                    designation = designation,
                    scopeSession = scopeSession,
                    lockProvider = moduleComponents.globalResolveComponents.lockProvider,
                    towerDataContextCollector = null,
                    firProviderInterceptor = null,
                )
            }
        }
    }

    internal fun runLazyDesignatedOnAirResolveToBodyWithoutLock(
        designation: FirDesignationWithFile,
        onAirCreatedDeclaration: Boolean,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        resolveFileToImportsWithoutLock(designation.firFile)
        var currentPhase = maxOf(designation.target.resolveState.resolvePhase, FirResolvePhase.IMPORTS)

        val scopeSession = ScopeSession()

        val firProviderInterceptor = if (onAirCreatedDeclaration) {
            LLFirFirProviderInterceptor.createForFirElement(
                session = designation.firFile.moduleData.session,
                firFile = designation.firFile,
                element = designation.target
            )
        } else null

        while (currentPhase < FirResolvePhase.BODY_RESOLVE) {
            currentPhase = currentPhase.next
            checkCanceled()

            LLFirLazyTransformerExecutor.execute(
                phase = currentPhase,
                designation = designation.asMultiDesignation(resolveMembersInsideTarget = true),
                scopeSession = scopeSession,
                lockProvider = moduleComponents.globalResolveComponents.lockProvider,
                towerDataContextCollector = towerDataContextCollector,
                firProviderInterceptor = firProviderInterceptor,
            )
        }
    }
}

private fun handleExceptionFromResolve(
    exception: Exception,
    sessionInvalidator: LLFirSessionInvalidator,
    firDeclarationToResolve: FirElementWithResolveState,
    fromPhase: FirResolvePhase,
    toPhase: FirResolvePhase?
): Nothing {
    sessionInvalidator.invalidate(firDeclarationToResolve.llFirSession)
    rethrowExceptionWithDetails(
        buildString {
            val moduleData = firDeclarationToResolve.llFirModuleData
            appendLine("Error while resolving ${firDeclarationToResolve::class.java.name} ")
            appendLine("from $fromPhase to $toPhase")
            appendLine("current declaration phase ${firDeclarationToResolve.resolveState}")
            appendLine("origin: ${(firDeclarationToResolve as? FirDeclaration)?.origin}")
            appendLine("session: ${firDeclarationToResolve.llFirSession::class}")
            appendLine("module data: ${moduleData::class}")
            appendLine("KtModule: ${moduleData.ktModule::class}")
            appendLine("platform: ${moduleData.ktModule.platform}")
        },
        exception = exception,
    ) {
        withEntry("KtModule", firDeclarationToResolve.llFirModuleData.ktModule) { it.moduleDescription }
        withEntry("session", firDeclarationToResolve.llFirSession) { it.toString() }
        withEntry("moduleData", firDeclarationToResolve.moduleData) { it.toString() }
        withFirEntry("firDeclarationToResolve", firDeclarationToResolve)
    }
}
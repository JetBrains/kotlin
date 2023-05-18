/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirClassWithAllMembersResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirSingleResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.InvalidSessionException
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyResolverRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.withOnAirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.rethrowExceptionWithDetails
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.visitors.transformSingle

internal class LLFirModuleLazyDeclarationResolver(val moduleComponents: LLFirModuleResolveComponents) {
    /**
     * Lazily resolves the [target] to a given [toPhase].
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration which is going to be resolved.
     *
     * Suitable for body resolve or/and on-air resolve.
     */
    fun lazyResolve(
        target: FirElementWithResolveState,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
    ) {
        val fromPhase = target.resolvePhase
        if (fromPhase >= toPhase) return
        try {
            resolveContainingFileToImports(target)
            if (toPhase == FirResolvePhase.IMPORTS) return

            lazyResolveTargets(
                targets = LLFirResolveMultiDesignationCollector.getDesignationsToResolve(target),
                scopeSession = scopeSession,
                toPhase = toPhase,
                towerDataContextCollector = null,
            )
        } catch (e: Exception) {
            handleExceptionFromResolve(e, target, fromPhase, toPhase)
        }
    }

    /**
     * Lazily resolves the [target] with all callable members to a given [toPhase].
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration which is going to be resolved.
     *
     * Suitable for body resolve or/and on-air resolve.
     */
    fun lazyResolveWithCallableMembers(
        target: FirRegularClass,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
    ) {
        val fromPhase = target.resolvePhase
        try {
            resolveContainingFileToImports(target)
            if (toPhase == FirResolvePhase.IMPORTS) return

            lazyResolveTargets(
                targets = LLFirResolveMultiDesignationCollector.getDesignationsToResolveWithCallableMembers(target),
                scopeSession = scopeSession,
                toPhase = toPhase,
                towerDataContextCollector = null,
            )
        } catch (e: Exception) {
            handleExceptionFromResolve(e, target, fromPhase, toPhase)
        }
    }


    /**
     * Lazily resolves all the declarations which are specified for resolve by [target]
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration which is going to be resolved.
     *
     * Suitable for body resolve or/and on-air resolve.
     */
    fun lazyResolveTarget(
        target: LLFirResolveTarget,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        try {
            lazyResolveTargets(
                targets = listOf(target),
                moduleComponents.scopeSessionProvider.getScopeSession(),
                toPhase,
                towerDataContextCollector,
            )
        } catch (e: Exception) {
            handleExceptionFromResolve(e, target, toPhase)
        }
    }

    fun runLazyDesignatedOnAirResolveToBodyWithoutLock(
        designation: FirDesignationWithFile,
        onAirCreatedDeclaration: Boolean,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        resolveFileToImportsWithLock(designation.firFile)

        val target = when (designation.target) {
            is FirRegularClass -> LLFirClassWithAllMembersResolveTarget(designation.firFile, designation.path, designation.target)
            else -> LLFirSingleResolveTarget(designation.firFile, designation.path, designation.target)
        }

        fun runTransformation() {
            val scopeSession = ScopeSession()
            lazyResolveTargets(listOf(target), scopeSession, FirResolvePhase.BODY_RESOLVE, towerDataContextCollector)
        }

        try {
            if (onAirCreatedDeclaration) {
                withOnAirDesignation(designation, ::runTransformation)
            } else {
                runTransformation()
            }
        } catch (e: Exception) {
            handleExceptionFromResolve(e, target, FirResolvePhase.BODY_RESOLVE)
        }
    }

    private fun resolveContainingFileToImports(target: FirElementWithResolveState) {
        if (target.resolvePhase >= FirResolvePhase.IMPORTS) return
        val firFile = target.getContainingFile() ?: return
        resolveFileToImportsWithLock(firFile)
    }

    private fun resolveFileToImportsWithLock(firFile: FirFile) {
        moduleComponents.globalResolveComponents.lockProvider.withWriteLock(firFile, FirResolvePhase.IMPORTS) {
            firFile.transformSingle(FirImportResolveTransformer(firFile.moduleData.session), null)
        }
    }

    private fun lazyResolveTargets(
        targets: List<LLFirResolveTarget>,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        if (targets.isEmpty()) return
        var currentPhase = getMinResolvePhase(targets).coerceAtLeast(FirResolvePhase.IMPORTS)
        if (currentPhase >= toPhase) return

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            checkCanceled()

            for (target in targets) {
                LLFirLazyResolverRunner.runLazyResolverByPhase(
                    phase = currentPhase,
                    target = target,
                    scopeSession = scopeSession,
                    lockProvider = moduleComponents.globalResolveComponents.lockProvider,
                    towerDataContextCollector = towerDataContextCollector,
                )
            }
        }
    }

    private fun getMinResolvePhase(designations: List<LLFirResolveTarget>): FirResolvePhase {
        var min = FirResolvePhase.BODY_RESOLVE
        for (designation in designations) {
            if (min == FirResolvePhase.RAW_FIR) break
            designation.forEachTarget { target ->
                min = minOf(min, target.resolvePhase)
            }
        }

        return min
    }
}

private fun handleExceptionFromResolve(
    exception: Exception,
    firDeclarationToResolve: FirElementWithResolveState,
    fromPhase: FirResolvePhase,
    toPhase: FirResolvePhase?
): Nothing {
    if (exception is InvalidSessionException) {
        throw exception
    }

    firDeclarationToResolve.llFirSession.invalidate()
    rethrowExceptionWithDetails(
        buildString {
            val moduleData = firDeclarationToResolve.llFirModuleData
            appendLine("Error while resolving ${firDeclarationToResolve::class.java.name} ")
            appendLine("from $fromPhase to $toPhase")
            appendLine("current declaration phase ${firDeclarationToResolve.resolvePhase}")
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

private fun handleExceptionFromResolve(
    exception: Exception,
    designation: LLFirResolveTarget,
    toPhase: FirResolvePhase?
): Nothing {
    if (exception is InvalidSessionException) {
        throw exception
    }

    val llFirSession = designation.firFile.llFirSession
    llFirSession.invalidate()
    val moduleData = llFirSession.llFirModuleData
    rethrowExceptionWithDetails(
        buildString {
            appendLine("Error while resolving ${designation::class.java.name} ")
            appendLine("to $toPhase")
            appendLine("module data: ${moduleData::class}")
            appendLine("KtModule: ${moduleData.ktModule::class}")
            appendLine("platform: ${moduleData.ktModule.platform}")
        },
        exception = exception,
    ) {
        withEntry("KtModule", moduleData.ktModule) { it.moduleDescription }
        withEntry("session", designation.firFile.llFirSession) { it.toString() }
        withEntry("moduleData", moduleData) { it.toString() }
        withEntry("firDesignationToResolve", designation) { it.toString() }
    }
}
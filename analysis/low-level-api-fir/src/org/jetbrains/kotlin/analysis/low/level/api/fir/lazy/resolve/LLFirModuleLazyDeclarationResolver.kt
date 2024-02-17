/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyResolverRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.utils.exceptions.rethrowExceptionWithDetails

internal class LLFirModuleLazyDeclarationResolver(val moduleComponents: LLFirModuleResolveComponents) {
    /**
     * Lazily resolves the [target] to a given [toPhase].
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration that is going to be resolved.
     */
    fun lazyResolve(target: FirElementWithResolveState, toPhase: FirResolvePhase) {
        if (target.resolvePhase >= toPhase) return

        lazyResolve(target, toPhase, LLFirResolveDesignationCollector::getDesignationToResolve)
    }

    /**
     * Lazily resolves the [target] with all callable members to a given [toPhase].
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration that is going to be resolved.
     */
    fun lazyResolveWithCallableMembers(target: FirRegularClass, toPhase: FirResolvePhase) {
        lazyResolve(target, toPhase, LLFirResolveDesignationCollector::getDesignationToResolveWithCallableMembers)
    }

    /**
     * Lazily resolves the [target] with nested declarations to a given [toPhase] recursively.
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration that is going to be resolved.
     */
    fun lazyResolveRecursively(target: FirElementWithResolveState, toPhase: FirResolvePhase) {
        lazyResolve(target, toPhase, LLFirResolveDesignationCollector::getDesignationToResolveRecursively)
    }

    private inline fun <T : FirElementWithResolveState> lazyResolve(
        targetElement: T,
        toPhase: FirResolvePhase,
        resolveTarget: (T) -> LLFirResolveTarget?,
    ) {
        val fromPhase = targetElement.resolvePhase
        try {
            resolveContainingFileToImports(targetElement)
            if (toPhase == FirResolvePhase.IMPORTS) return

            val target = resolveTarget(targetElement) ?: return
            lazyResolveTargets(target, toPhase)
        } catch (e: Exception) {
            handleExceptionFromResolve(e, targetElement, fromPhase, toPhase)
        }
    }


    /**
     * Lazily resolves all the declarations which are specified for resolve by [target]
     *
     * Might resolve additional required declarations.
     *
     * Resolution is performed under the lock specific to each declaration which is going to be resolved.
     */
    fun lazyResolveTarget(
        target: LLFirResolveTarget,
        toPhase: FirResolvePhase,
    ) {
        try {
            target.firFile?.let(::resolveFileToImportsWithLock)
            if (toPhase == FirResolvePhase.IMPORTS) return

            lazyResolveTargets(target, toPhase)
        } catch (e: Exception) {
            handleExceptionFromResolve(e, target, toPhase)
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

    private fun lazyResolveTargets(target: LLFirResolveTarget, toPhase: FirResolvePhase) {
        var currentPhase = getMinResolvePhase(target).coerceAtLeast(FirResolvePhase.IMPORTS)
        if (currentPhase >= toPhase) return

        // to catch a contract violation for jumping phases
        moduleComponents.globalResolveComponents.lockProvider.checkContractViolations(toPhase)

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            checkCanceled()

            LLFirLazyResolverRunner.runLazyResolverByPhase(
                phase = currentPhase,
                target = target,
            )
        }
    }

    private fun getMinResolvePhase(designation: LLFirResolveTarget): FirResolvePhase {
        var min = FirResolvePhase.BODY_RESOLVE
        designation.forEachTarget { target ->
            min = minOf(min, target.resolvePhase)
        }

        return min
    }
}

private fun handleExceptionFromResolve(
    exception: Exception,
    firDeclarationToResolve: FirElementWithResolveState,
    fromPhase: FirResolvePhase,
    toPhase: FirResolvePhase,
): Nothing {
    val session = firDeclarationToResolve.llFirSession
    val moduleData = firDeclarationToResolve.llFirModuleData
    val module = moduleData.ktModule

    rethrowExceptionWithDetails(
        buildString {
            appendLine("Error while resolving ${firDeclarationToResolve::class.java.name} ")
            appendLine("from $fromPhase to $toPhase")
            appendLine("current declaration phase ${firDeclarationToResolve.resolvePhase}")
            appendLine("origin: ${(firDeclarationToResolve as? FirDeclaration)?.origin}")
            appendLine("session: ${session::class}")
            appendLine("module data: ${moduleData::class}")
            appendLine("KtModule: ${module::class}")
            appendLine("platform: ${module.platform}")
        },
        exception = exception,
    ) {
        withEntry("KtModule", module) { it.moduleDescription }
        withEntry("session", session) { it.toString() }
        withEntry("moduleData", firDeclarationToResolve.moduleData) { it.toString() }
        withFirEntry("firDeclarationToResolve", firDeclarationToResolve)
    }
}

private fun handleExceptionFromResolve(
    exception: Exception,
    designation: LLFirResolveTarget,
    toPhase: FirResolvePhase,
): Nothing {
    val session = designation.target.llFirSession
    val moduleData = session.llFirModuleData
    val module = moduleData.ktModule

    rethrowExceptionWithDetails(
        buildString {
            appendLine("Error while resolving ${designation::class.java.name} ")
            appendLine("to $toPhase")
            appendLine("module data: ${moduleData::class}")
            appendLine("KtModule: ${module::class}")
            appendLine("platform: ${module.platform}")
        },
        exception = exception,
    ) {
        withEntry("KtModule", module) { it.moduleDescription }
        withEntry("session", session) { it.toString() }
        withEntry("moduleData", moduleData) { it.toString() }
        withEntry("firDesignationToResolve", designation) { it.toString() }
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithFile
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
import org.jetbrains.kotlin.fir.FirElementWithResolvePhase
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

internal class LLFirModuleLazyDeclarationResolver(val moduleComponents: LLFirModuleResolveComponents) {

    private fun FirDeclaration.isValidForResolve(): Boolean = when (origin) {
        is FirDeclarationOrigin.Source,
        is FirDeclarationOrigin.ImportedFromObjectOrStatic,
        is FirDeclarationOrigin.Delegated,
        is FirDeclarationOrigin.Synthetic,
        is FirDeclarationOrigin.SubstitutionOverride,
        is FirDeclarationOrigin.SamConstructor,
        is FirDeclarationOrigin.IntersectionOverride -> {
            when (this) {
                is FirFile -> true
                is FirSyntheticProperty -> false
                is FirSyntheticPropertyAccessor -> false
                is FirSimpleFunction,
                is FirProperty,
                is FirPropertyAccessor,
                is FirField,
                is FirTypeAlias,
                is FirConstructor -> resolvePhase < FirResolvePhase.BODY_RESOLVE
                else -> true
            }
        }
        else -> {
            check(resolvePhase == FirResolvePhase.BODY_RESOLVE) {
                "Expected body resolve phase for origin $origin but found $resolvePhase"
            }
            false
        }
    }

    private fun FirElementWithResolvePhase.isValidForResolve() = when (this) {
        is FirDeclaration -> isValidForResolve()
        is FirFileAnnotationsContainer -> true
        else -> throwUnexpectedFirElementError(this)
    }

    private fun resolveContainingFileToImports(target: FirElementWithResolvePhase) {
        if (target.resolvePhase >= FirResolvePhase.IMPORTS) return
        val firFile = target.getContainingFile() ?: return
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS) return
        moduleComponents.globalResolveComponents.lockProvider.withLock(firFile) {
            resolveFileToImportsWithoutLock(firFile)
        }
    }

    private fun resolveFileToImportsWithoutLock(firFile: FirFile) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS) return
        checkCanceled()
        firFile.transform<FirElement, Any?>(FirImportResolveTransformer(firFile.moduleData.session), null)
        firFile.replaceResolvePhase(FirResolvePhase.IMPORTS)
    }

    /**
     * Run designated resolve only designation with fully resolved path (synchronized).
     * Suitable for body resolve or/and on-air resolve.
     * @see lazyResolve for ordinary resolve
     * @param target target non-local declaration
     */
    fun lazyResolve(
        target: FirElementWithResolvePhase,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
    ) {
        val fromPhase = target.resolvePhase
        try {
            doLazyResolve(target, scopeSession, toPhase)
        } catch (e: Exception) {
            handleExceptionFromResolve(e, moduleComponents.sessionInvalidator, target, fromPhase, toPhase)
        }
    }

    private fun doLazyResolve(
        target: FirElementWithResolvePhase,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
    ) {
        if (target.resolvePhase >= toPhase) return
        resolveContainingFileToImports(target)
        if (toPhase == FirResolvePhase.IMPORTS) return

        for (designation in declarationDesignationsToResolve(target)) {
            if (!designation.target.isValidForResolve()) continue
            if (designation.target.resolvePhase >= toPhase) continue
            moduleComponents.globalResolveComponents.lockProvider.withLock(designation.firFile) {
                runLazyDesignatedResolveWithoutLock(
                    designation = designation,
                    scopeSession = scopeSession,
                    toPhase = toPhase,
                )
            }
        }
    }

    private fun declarationDesignationsToResolve(target: FirElementWithResolvePhase): List<FirDesignationWithFile> {
        return when (target) {
            is FirPropertyAccessor -> declarationDesignationsToResolve(target.propertySymbol.fir)
            is FirBackingField -> declarationDesignationsToResolve(target.propertySymbol.fir)
            is FirTypeParameter -> declarationDesignationsToResolve(target.containingDeclarationSymbol.fir)
            is FirValueParameter -> declarationDesignationsToResolve(target.containingFunctionSymbol.fir)
            is FirFile -> {
                val validForResolveDeclarations = buildList {
                    add(target.annotationsContainer)
                    target.declarations.filterTo(this) { it.isValidForResolve() }
                }

                validForResolveDeclarations.map {
                    FirDesignationWithFile(path = emptyList(), target = it, firFile = target)
                }
            }
            else -> listOfNotNull(target.tryCollectDesignationWithFile())
        }
    }


    private fun runLazyDesignatedResolveWithoutLock(
        designation: FirDesignationWithFile,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
    ) {
        val declarationResolvePhase = designation.target.resolvePhase
        if (declarationResolvePhase >= toPhase) return

        var currentPhase = maxOf(declarationResolvePhase, FirResolvePhase.IMPORTS)

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            checkCanceled()

            LLFirLazyTransformerExecutor.execute(
                phase = currentPhase,
                designation = designation,
                scopeSession = scopeSession,
                phaseRunner = moduleComponents.globalResolveComponents.phaseRunner,
                lockProvider = moduleComponents.globalResolveComponents.lockProvider,
                towerDataContextCollector = null,
                firProviderInterceptor = null,
            )
        }
    }

    internal fun runLazyDesignatedOnAirResolveToBodyWithoutLock(
        designation: FirDesignationWithFile,
        onAirCreatedDeclaration: Boolean,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        resolveFileToImportsWithoutLock(designation.firFile)
        var currentPhase = maxOf(designation.target.resolvePhase, FirResolvePhase.IMPORTS)

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
                designation = designation,
                scopeSession = scopeSession,
                phaseRunner = moduleComponents.globalResolveComponents.phaseRunner,
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
    firDeclarationToResolve: FirElementWithResolvePhase,
    fromPhase: FirResolvePhase,
    toPhase: FirResolvePhase?
): Nothing {
    sessionInvalidator.invalidate(firDeclarationToResolve.llFirSession)
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
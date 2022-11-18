/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidator
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirFirProviderInterceptor
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformerExecutor
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ktDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.rethrowExceptionWithDetails
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolvePhase
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.SourceCodeAnalysisException
import org.jetbrains.kotlin.util.shouldIjPlatformExceptionBeRethrown
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

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

    private fun lazyResolveFileDeclaration(
        firFile: FirFile,
        toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        collector: FirTowerDataContextCollector? = null,
    ) {
        val fromPhase = firFile.resolvePhase
        try {
            if (toPhase == FirResolvePhase.RAW_FIR) return
            resolveFileToImports(firFile)
            if (toPhase == FirResolvePhase.IMPORTS) return
            if (firFile.resolvePhase >= toPhase) return
            moduleComponents.globalResolveComponents.lockProvider.runCustomResolveUnderLock(firFile) {
                lazyResolveFileDeclarationWithoutLock(
                    firFile = firFile,
                    toPhase = toPhase,
                    collector = collector,
                    scopeSession = scopeSession,
                )
            }
        } catch (e: Exception) {
            handleExceptionFromResolve(e, moduleComponents.sessionInvalidator, firFile, fromPhase, toPhase)
        }
    }

    private fun resolveFileToImports(firFile: FirFile) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS) return
        moduleComponents.globalResolveComponents.lockProvider.runCustomResolveUnderLock(firFile) {
            resolveFileToImportsWithoutLock(firFile)
        }
    }

    private fun resolveFileToImportsWithoutLock(firFile: FirFile) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS) return
        checkCanceled()
        firFile.transform<FirElement, Any?>(FirImportResolveTransformer(firFile.moduleData.session), null)
        firFile.replaceResolvePhase(FirResolvePhase.IMPORTS)

    }

    private fun lazyResolveFileDeclarationWithoutLock(
        firFile: FirFile,
        toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        collector: FirTowerDataContextCollector? = null,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        resolveFileToImportsWithoutLock(firFile)
        if (toPhase == FirResolvePhase.IMPORTS) return
        checkCanceled()

        val validForResolveDeclarations = buildList {
            add(firFile.annotationsContainer)
            firFile.declarations.filterTo(this) { it.isValidForResolve() }
        }.filter { it.resolvePhase < toPhase }


        if (validForResolveDeclarations.isEmpty()) return
        val designations = validForResolveDeclarations.map {
            FirDesignationWithFile(path = emptyList(), target = it, firFile = firFile)
        }

        var currentPhase = FirResolvePhase.IMPORTS
        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            checkCanceled()

            val transformersToApply = designations.filter { designation ->
                designation.target.resolvePhase < currentPhase
            }

            if (transformersToApply.isEmpty()) continue

            moduleComponents.globalResolveComponents.phaseRunner.runPhaseWithCustomResolve(currentPhase) {
                for (curDesignation in transformersToApply) {
                    checkCanceled()
                    LLFirLazyTransformerExecutor.execute(
                        phase = currentPhase,
                        designation = curDesignation,
                        scopeSession = scopeSession,
                        phaseRunner = moduleComponents.globalResolveComponents.phaseRunner,
                        lockProvider = moduleComponents.globalResolveComponents.lockProvider,
                        towerDataContextCollector = collector,
                        firProviderInterceptor = null,
                    )
                }
            }
            firFile.replaceResolvePhase(currentPhase)
        }
    }

    private fun fastTrackForImportsPhase(target: FirElementWithResolvePhase): Boolean {
        val provider = target.moduleData.session.firProvider
        val firFile = when (target) {
            is FirFile -> target
            is FirCallableDeclaration -> provider.getFirCallableContainerFile(target.symbol)
            is FirClassLikeDeclaration -> provider.getFirClassifierContainerFile(target.symbol)
            else -> null
        } ?: return false
        resolveFileToImports(firFile)
        return true
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
        if (toPhase == FirResolvePhase.RAW_FIR) return
        if (toPhase == FirResolvePhase.IMPORTS) {
            if (fastTrackForImportsPhase(target)) {
                return
            }
        }
        when (target) {
            is FirSyntheticPropertyAccessor -> {
                lazyResolve(target.delegate, scopeSession, toPhase)
                return
            }

            is FirBackingField -> {
                lazyResolve(target.propertySymbol.fir, scopeSession, toPhase)
                return
            }

            is FirFile -> {
                lazyResolveFileDeclaration(target, toPhase, scopeSession)
                return
            }
            else -> {}
        }

        if (target is FirDeclaration && !target.isValidForResolve()) return
        if (target.resolvePhase >= toPhase) return

        val requestedDeclarationDesignation = target.tryCollectDesignationWithFile()

        val designation: FirDesignationWithFile
        val neededPhase: FirResolvePhase

        if (target is FirDanglingModifierList) {
            neededPhase = toPhase
            val ktFile = target.psi?.containingFile as? KtFile
                ?: error("File for dangling modifier list cannot be null")
            designation = FirDesignationWithFile(
                emptyList(),
                target,
                moduleComponents.cache.getCachedFirFile(ktFile) ?: error("Fir file for dandling modifier list cannot be null")
            )
        } else if (requestedDeclarationDesignation != null) {
            designation = requestedDeclarationDesignation
            neededPhase = toPhase
        } else {
            val possiblyLocalDeclaration = (target as FirDeclaration).getKtDeclarationForFirElement()
            val nonLocalDeclaration = possiblyLocalDeclaration.getNonLocalContainingOrThisDeclaration()
                ?: error("Container for local declaration cannot be null")
            val isLocalDeclarationResolveRequested =
                possiblyLocalDeclaration != nonLocalDeclaration
            val isValueParameterInsidePrimaryConstructor = target is FirValueParameter
                    && possiblyLocalDeclaration is KtPrimaryConstructor

            val declarationToResolve: FirDeclaration

            if (isLocalDeclarationResolveRequested && !isValueParameterInsidePrimaryConstructor) {
                val enumEntry = possiblyLocalDeclaration.getContainingEnumEntryAsMemberOfEnumEntry() ?: return

                declarationToResolve = enumEntry.findSourceNonLocalFirDeclaration(
                    moduleComponents.firFileBuilder,
                    target.moduleData.session.firProvider,
                )
                neededPhase = FirResolvePhase.BODY_RESOLVE
            } else {
                declarationToResolve = nonLocalDeclaration.findSourceNonLocalFirDeclaration(
                    moduleComponents.firFileBuilder,
                    target.moduleData.session.firProvider,
                )
                neededPhase = toPhase
            }

            if (declarationToResolve.resolvePhase >= neededPhase) return
            if (!declarationToResolve.isValidForResolve()) return

            designation = declarationToResolve.collectDesignationWithFile()
        }

        if (designation.target.resolvePhase >= neededPhase) return

        if (neededPhase == FirResolvePhase.IMPORTS) {
            resolveFileToImports(designation.firFile)
            return
        }

        moduleComponents.globalResolveComponents.lockProvider.runCustomResolveUnderLock(designation.firFile) {
            runLazyDesignatedResolveWithoutLock(
                designation = designation,
                scopeSession = scopeSession,
                toPhase = neededPhase,
            )
            designation.target
        }
    }


    private fun runLazyDesignatedResolveWithoutLock(
        designation: FirDesignationWithFile,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        resolveFileToImportsWithoutLock(designation.firFile)
        if (toPhase == FirResolvePhase.IMPORTS) return

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

private fun KtDeclaration.getContainingEnumEntryAsMemberOfEnumEntry(): KtEnumEntry? {
    val body = parent as? KtClassBody ?: return null
    return body.parent as? KtEnumEntry
}


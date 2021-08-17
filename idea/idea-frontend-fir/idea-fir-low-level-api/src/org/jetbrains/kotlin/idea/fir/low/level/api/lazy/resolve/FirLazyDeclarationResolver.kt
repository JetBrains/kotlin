/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolveStatus
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.tryCollectDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirFileAnnotationsResolveTransformer
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirProviderInterceptorForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.LazyTransformerFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.util.FirElementFinder
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration

internal class FirLazyDeclarationResolver(private val firFileBuilder: FirFileBuilder) {
    /**
     * Fully resolve file annotations (synchronized)
     * @see resolveFileAnnotationsWithoutLock not synchronized
     */
    fun resolveFileAnnotations(
        firFile: FirFile,
        annotations: List<FirAnnotationCall>,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
        checkPCE: Boolean,
        collector: FirTowerDataContextCollector? = null,
    ) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS && annotations.all { it.resolveStatus == FirAnnotationResolveStatus.Resolved }) return
        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
            resolveFileAnnotationsWithoutLock(
                firFile = firFile,
                annotations = annotations,
                scopeSession = scopeSession,
                collector = collector
            )
        }
    }

    /**
     * Fully resolve file annotations (not synchronized)
     * @see resolveFileAnnotations synchronized version
     */
    private fun resolveFileAnnotationsWithoutLock(
        firFile: FirFile,
        annotations: List<FirAnnotationCall>,
        scopeSession: ScopeSession,
        collector: FirTowerDataContextCollector? = null,
    ) {
        if (firFile.resolvePhase < FirResolvePhase.IMPORTS) {
            resolveFileToImportsWithoutLock(firFile, false)
        }

        if (!annotations.all { it.resolveStatus == FirAnnotationResolveStatus.Resolved }) {
            FirFileAnnotationsResolveTransformer(
                firFile = firFile,
                annotations = annotations,
                session = firFile.moduleData.session,
                scopeSession = scopeSession,
                firTowerDataContextCollector = collector,
            ).transformDeclaration(firFileBuilder.firPhaseRunner)
        }
    }

    private fun FirDeclaration.isValidForResolve(): Boolean = when (origin) {
        is FirDeclarationOrigin.Source,
        is FirDeclarationOrigin.ImportedFromObject,
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

    fun lazyResolveFileDeclaration(
        firFile: FirFile,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        checkPCE: Boolean = false,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        resolveFileToImports(firFile, moduleFileCache, checkPCE)
        if (toPhase == FirResolvePhase.IMPORTS) return
        if (firFile.resolvePhase >= toPhase) return
        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
            lazyResolveFileDeclarationWithoutLock(
                firFile = firFile,
                moduleFileCache = moduleFileCache,
                toPhase = toPhase,
                scopeSession = scopeSession,
                checkPCE = checkPCE,
            )
        }
    }

    private fun resolveFileToImports(firFile: FirFile, moduleFileCache: ModuleFileCache, checkPCE: Boolean) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS) return
        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
            resolveFileToImportsWithoutLock(firFile, checkPCE)
        }
    }

    private fun resolveFileToImportsWithoutLock(firFile: FirFile, checkPCE: Boolean) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS) return
        if (checkPCE) checkCanceled()
        firFile.transform<FirElement, Any?>(FirImportResolveTransformer(firFile.moduleData.session), null)
        firFile.replaceResolvePhase(FirResolvePhase.IMPORTS)
    }

    private fun lazyResolveFileDeclarationWithoutLock(
        firFile: FirFile,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        checkPCE: Boolean = false,
        collector: FirTowerDataContextCollector? = null,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        resolveFileToImportsWithoutLock(firFile, checkPCE)
        if (toPhase == FirResolvePhase.IMPORTS) return
        if (checkPCE) checkCanceled()

        resolveFileAnnotationsWithoutLock(firFile, firFile.annotations, scopeSession, collector)

        val validForResolveDeclarations = firFile.declarations
            .filter { it.isValidForResolve() && it.resolvePhase < toPhase }

        if (validForResolveDeclarations.isEmpty()) return
        val designations = validForResolveDeclarations.map {
            FirDeclarationDesignationWithFile(path = emptyList(), declaration = it, firFile = firFile)
        }

        var currentPhase = FirResolvePhase.IMPORTS
        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()

            val transformersToApply = designations.mapNotNull {
                val needToResolve = it.declaration.resolvePhase < currentPhase
                if (needToResolve) {
                    LazyTransformerFactory.createLazyTransformer(
                        phase = currentPhase,
                        designation = it,
                        scopeSession = scopeSession,
                        moduleFileCache = moduleFileCache,
                        lazyDeclarationResolver = this,
                        towerDataContextCollector = null,
                        firProviderInterceptor = null,
                        checkPCE = checkPCE,
                    )
                } else null
            }
            if (transformersToApply.isEmpty()) continue

            firFileBuilder.firPhaseRunner.runPhaseWithCustomResolve(currentPhase) {
                for (currentTransformer in transformersToApply) {
                    currentTransformer.transformDeclaration(firFileBuilder.firPhaseRunner)
                }
            }
            firFile.replaceResolvePhase(currentPhase)
        }
    }

    private fun fastTrackForImportsPhase(
        firDeclarationToResolve: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        checkPCE: Boolean,
    ): Boolean {
        val provider = firDeclarationToResolve.moduleData.session.firProvider
        val firFile = when (firDeclarationToResolve) {
            is FirFile -> firDeclarationToResolve
            is FirCallableDeclaration -> provider.getFirCallableContainerFile(firDeclarationToResolve.symbol)
            is FirClassLikeDeclaration -> provider.getFirClassifierContainerFile(firDeclarationToResolve.symbol)
            else -> null
        } ?: return false
        resolveFileToImports(firFile, moduleFileCache, checkPCE)
        return true
    }

    /**
     * Run designated resolve only designation with fully resolved path (synchronized).
     * Suitable for body resolve or/and on-air resolve.
     * @see lazyResolveDeclaration for ordinary resolve
     * @param firDeclarationToResolve target non-local declaration
     */
    fun <D : FirDeclaration> lazyResolveDeclaration(
        firDeclarationToResolve: D,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
        skipLocalDeclaration: Boolean = false,
    ): D {
        if (toPhase == FirResolvePhase.RAW_FIR) return firDeclarationToResolve
        if (toPhase == FirResolvePhase.IMPORTS) {
            if (fastTrackForImportsPhase(firDeclarationToResolve, moduleFileCache, checkPCE)) {
                return firDeclarationToResolve
            }
        }
        if (!firDeclarationToResolve.isValidForResolve()) return firDeclarationToResolve
        if (firDeclarationToResolve.resolvePhase >= toPhase) return firDeclarationToResolve

        if (firDeclarationToResolve is FirFile) {
            lazyResolveFileDeclaration(
                firFile = firDeclarationToResolve,
                moduleFileCache = moduleFileCache,
                toPhase = toPhase,
                scopeSession = scopeSession,
                checkPCE = checkPCE,
            )
            return firDeclarationToResolve
        }

        val requestedDeclarationDesignation = firDeclarationToResolve.tryCollectDesignationWithFile()

        val designation: FirDeclarationDesignationWithFile
        val neededPhase: FirResolvePhase
        val isLocalDeclarationResolveRequested: Boolean
        if (requestedDeclarationDesignation != null) {
            designation = requestedDeclarationDesignation
            neededPhase = toPhase
            isLocalDeclarationResolveRequested = false
        } else {
            val possiblyLocalDeclaration = firDeclarationToResolve.getKtDeclarationForFirElement()
            val nonLocalDeclaration = possiblyLocalDeclaration.getNonLocalContainingOrThisDeclaration()
                ?: error("Container for local declaration cannot be null")

            isLocalDeclarationResolveRequested = possiblyLocalDeclaration != nonLocalDeclaration
            if (isLocalDeclarationResolveRequested && skipLocalDeclaration) return firDeclarationToResolve

            val nonLocalFirDeclaration = nonLocalDeclaration.findSourceNonLocalFirDeclaration(
                firFileBuilder,
                firDeclarationToResolve.moduleData.session.firIdeProvider.symbolProvider,
                moduleFileCache
            )

            neededPhase = if (isLocalDeclarationResolveRequested) FirResolvePhase.BODY_RESOLVE else toPhase

            if (nonLocalFirDeclaration.resolvePhase >= neededPhase) return firDeclarationToResolve
            if (!nonLocalFirDeclaration.isValidForResolve()) return firDeclarationToResolve

            designation = nonLocalFirDeclaration.collectDesignationWithFile()
        }

        if (designation.declaration.resolvePhase >= neededPhase) return firDeclarationToResolve

        if (neededPhase == FirResolvePhase.IMPORTS) {
            resolveFileToImports(designation.firFile, moduleFileCache, checkPCE)
            return firDeclarationToResolve
        }

        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(designation.firFile, checkPCE) {
            runLazyDesignatedResolveWithoutLock(
                designation = designation,
                moduleFileCache = moduleFileCache,
                scopeSession = scopeSession,
                toPhase = neededPhase,
                checkPCE = checkPCE,
            )
        }

        if (!isLocalDeclarationResolveRequested) return firDeclarationToResolve
        return remapDeclarationInContainerIfNeeded(firDeclarationToResolve, designation.declaration, toPhase)
    }

    private fun <D : FirDeclaration> remapDeclarationInContainerIfNeeded(
        declarationToRemap: D,
        firContainer: FirDeclaration,
        firResolvePhase: FirResolvePhase
    ): D {
        if (declarationToRemap.resolvePhase >= firResolvePhase) return declarationToRemap
        val realPsi = declarationToRemap.realPsi
        check(realPsi != null) {
            "Cannot remap element without PSI"
        }
        @Suppress("UNCHECKED_CAST")
        val firDeclaration = FirElementFinder.findElementIn<FirDeclaration>(firContainer) {
            it.realPsi == realPsi
        } as? D
        check(firDeclaration != null) {
            "Containing declaration was resolved but local didn't found in it"
        }
        check(firDeclaration.resolvePhase >= firResolvePhase) {
            "Found local declaration wasn't completely resolved"
        }
        return firDeclaration
    }

    private fun runLazyDesignatedResolveWithoutLock(
        designation: FirDeclarationDesignationWithFile,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        resolveFileToImportsWithoutLock(designation.firFile, checkPCE)
        if (toPhase == FirResolvePhase.IMPORTS) return

        val declarationResolvePhase = designation.declaration.resolvePhase
        if (declarationResolvePhase >= toPhase) return

        var currentPhase = maxOf(declarationResolvePhase, FirResolvePhase.IMPORTS)

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()

            LazyTransformerFactory.createLazyTransformer(
                phase = currentPhase,
                designation = designation,
                scopeSession = scopeSession,
                moduleFileCache = moduleFileCache,
                lazyDeclarationResolver = this,
                towerDataContextCollector = null,
                firProviderInterceptor = null,
                checkPCE = checkPCE,
            ).transformDeclaration(firFileBuilder.firPhaseRunner)
        }
    }

    internal fun runLazyDesignatedOnAirResolveToBodyWithoutLock(
        designation: FirDeclarationDesignationWithFile,
        moduleFileCache: ModuleFileCache,
        checkPCE: Boolean,
        onAirCreatedDeclaration: Boolean,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        resolveFileToImportsWithoutLock(designation.firFile, checkPCE)
        var currentPhase = maxOf(designation.declaration.resolvePhase, FirResolvePhase.IMPORTS)

        val scopeSession = ScopeSession()

        val firProviderInterceptor = if (onAirCreatedDeclaration) {
            FirProviderInterceptorForIDE.createForFirElement(
                session = designation.firFile.moduleData.session,
                firFile = designation.firFile,
                element = designation.declaration
            )
        } else null

        while (currentPhase < FirResolvePhase.BODY_RESOLVE) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()

            LazyTransformerFactory.createLazyTransformer(
                phase = currentPhase,
                designation = designation,
                scopeSession = scopeSession,
                moduleFileCache = moduleFileCache,
                lazyDeclarationResolver = this,
                towerDataContextCollector = towerDataContextCollector,
                firProviderInterceptor = firProviderInterceptor,
                checkPCE = checkPCE,
            ).transformDeclaration(firFileBuilder.firPhaseRunner)
        }
    }
}



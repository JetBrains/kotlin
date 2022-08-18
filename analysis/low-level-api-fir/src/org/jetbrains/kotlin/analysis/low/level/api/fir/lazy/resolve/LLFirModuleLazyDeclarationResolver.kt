/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirFileAnnotationsResolveTransformer
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirFirProviderInterceptor
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LazyTransformerFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.shouldIjPlatformExceptionBeRethrown
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment

internal class LLFirModuleLazyDeclarationResolver(val moduleComponents: LLFirModuleResolveComponents) {
    /**
     * Fully resolve file annotations (synchronized)
     * @see resolveFileAnnotationsWithoutLock not synchronized
     */
    fun resolveFileAnnotations(
        firFile: FirFile,
        annotations: List<FirAnnotation>,
        scopeSession: ScopeSession,
        checkPCE: Boolean,
        collector: FirTowerDataContextCollector? = null,
    ) {
        val fromPhase = firFile.resolvePhase
        try {
            if (firFile.resolvePhase >= FirResolvePhase.IMPORTS && annotations.all { it.resolved }) return
            moduleComponents.globalResolveComponents.lockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
                resolveFileAnnotationsWithoutLock(
                    firFile = firFile,
                    annotations = annotations,
                    scopeSession = scopeSession,
                    collector = collector
                )
            }
        } catch (e: Throwable) {
            rethrowWithDetails(e, firFile, fromPhase, toPhase = null)
        }
    }

    /**
     * Fully resolve file annotations (not synchronized)
     * @see resolveFileAnnotations synchronized version
     */
    private fun resolveFileAnnotationsWithoutLock(
        firFile: FirFile,
        annotations: List<FirAnnotation>,
        scopeSession: ScopeSession,
        collector: FirTowerDataContextCollector? = null,
    ) {
        if (firFile.resolvePhase < FirResolvePhase.IMPORTS) {
            resolveFileToImportsWithoutLock(firFile, false)
        }

        if (!annotations.all { it.resolved }) {
            LLFirFileAnnotationsResolveTransformer(
                firFile = firFile,
                annotations = annotations,
                session = firFile.moduleData.session,
                scopeSession = scopeSession,
                firTowerDataContextCollector = collector,
            ).transformDeclaration(moduleComponents.globalResolveComponents.phaseRunner)
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
        toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        collector: FirTowerDataContextCollector? = null,
        checkPCE: Boolean = false,
    ) {
        val fromPhase = firFile.resolvePhase
        try {
            if (toPhase == FirResolvePhase.RAW_FIR) return
            resolveFileToImports(firFile, checkPCE)
            if (toPhase == FirResolvePhase.IMPORTS) return
            if (firFile.resolvePhase >= toPhase) return
            moduleComponents.globalResolveComponents.lockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
                ResolveTreeBuilder.resolveEnsure(firFile, toPhase) {
                    lazyResolveFileDeclarationWithoutLock(
                        firFile = firFile,
                        toPhase = toPhase,
                        collector = collector,
                        scopeSession = scopeSession,
                        checkPCE = checkPCE,
                    )
                }
            }
        } catch (e: Throwable) {
            rethrowWithDetails(e, firFile, fromPhase, toPhase)
        }
    }

    private fun resolveFileToImports(firFile: FirFile, checkPCE: Boolean) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS) return
        moduleComponents.globalResolveComponents.lockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
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
            if (checkPCE) checkCanceled()

            val transformersToApply = designations.mapNotNull {
                val needToResolve = it.declaration.resolvePhase < currentPhase
                if (needToResolve) {
                    LazyTransformerFactory.createLazyTransformer(
                        phase = currentPhase,
                        designation = it,
                        scopeSession = scopeSession,
                        lazyDeclarationResolver = this,
                        towerDataContextCollector = collector,
                        firProviderInterceptor = null,
                        checkPCE = checkPCE,
                    )
                } else null
            }
            if (transformersToApply.isEmpty()) continue

            moduleComponents.globalResolveComponents.phaseRunner.runPhaseWithCustomResolve(currentPhase) {
                for (currentTransformer in transformersToApply) {
                    currentTransformer.transformDeclaration(moduleComponents.globalResolveComponents.phaseRunner)
                }
            }
            firFile.replaceResolvePhase(currentPhase)
        }
    }

    private fun fastTrackForImportsPhase(
        firDeclarationToResolve: FirDeclaration,
        checkPCE: Boolean,
    ): Boolean {
        val provider = firDeclarationToResolve.moduleData.session.firProvider
        val firFile = when (firDeclarationToResolve) {
            is FirFile -> firDeclarationToResolve
            is FirCallableDeclaration -> provider.getFirCallableContainerFile(firDeclarationToResolve.symbol)
            is FirClassLikeDeclaration -> provider.getFirClassifierContainerFile(firDeclarationToResolve.symbol)
            else -> null
        } ?: return false
        resolveFileToImports(firFile, checkPCE)
        return true
    }

    /**
     * Run designated resolve only designation with fully resolved path (synchronized).
     * Suitable for body resolve or/and on-air resolve.
     * @see lazyResolveDeclaration for ordinary resolve
     * @param firDeclarationToResolve target non-local declaration
     */
    fun lazyResolveDeclaration(
        firDeclarationToResolve: FirDeclaration,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
    ) {
        val fromPhase = firDeclarationToResolve.resolvePhase
        try {
            doLazyResolveDeclaration(firDeclarationToResolve, scopeSession, toPhase, checkPCE)
        } catch (e: Throwable) {
            rethrowWithDetails(e, firDeclarationToResolve, fromPhase, toPhase)
        }
    }

    private fun doLazyResolveDeclaration(
        firDeclarationToResolve: FirDeclaration,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        if (toPhase == FirResolvePhase.IMPORTS) {
            if (fastTrackForImportsPhase(firDeclarationToResolve, checkPCE)) {
                return
            }
        }
        when (firDeclarationToResolve) {
            is FirSyntheticPropertyAccessor -> {
                lazyResolveDeclaration(
                    firDeclarationToResolve.delegate,
                    scopeSession,
                    toPhase,
                    checkPCE,
                )
                return
            }

            is FirBackingField -> {
                lazyResolveDeclaration(
                    firDeclarationToResolve.propertySymbol.fir,
                    scopeSession,
                    toPhase,
                    checkPCE,
                )
            }

            is FirFile -> {
                lazyResolveFileDeclaration(
                    firFile = firDeclarationToResolve,
                    toPhase = toPhase,
                    scopeSession = scopeSession,
                    checkPCE = checkPCE,
                )
                return
            }
            else -> {}
        }

        if (!firDeclarationToResolve.isValidForResolve()) return
        if (firDeclarationToResolve.resolvePhase >= toPhase) return


        val requestedDeclarationDesignation = firDeclarationToResolve.tryCollectDesignationWithFile()

        val designation: FirDeclarationDesignationWithFile
        val neededPhase: FirResolvePhase

        if (requestedDeclarationDesignation != null) {
            designation = requestedDeclarationDesignation
            neededPhase = toPhase
        } else {
            val possiblyLocalDeclaration = firDeclarationToResolve.getKtDeclarationForFirElement()
            val nonLocalDeclaration = possiblyLocalDeclaration.getNonLocalContainingOrThisDeclaration()
                ?: error("Container for local declaration cannot be null")
            val isLocalDeclarationResolveRequested =
                possiblyLocalDeclaration != nonLocalDeclaration

            val declarationToResolve: FirDeclaration

            if (isLocalDeclarationResolveRequested) {
                val enumEntry = possiblyLocalDeclaration.getContainingEnumEntryAsMemberOfEnumEntry() ?: return

                declarationToResolve = enumEntry.findSourceNonLocalFirDeclaration(
                    moduleComponents.firFileBuilder,
                    firDeclarationToResolve.moduleData.session.firProvider,
                )
                neededPhase = FirResolvePhase.BODY_RESOLVE
            } else {
                declarationToResolve = nonLocalDeclaration.findSourceNonLocalFirDeclaration(
                    moduleComponents.firFileBuilder,
                    firDeclarationToResolve.moduleData.session.firProvider,
                )
                neededPhase = toPhase
            }

            if (declarationToResolve.resolvePhase >= neededPhase) return
            if (!declarationToResolve.isValidForResolve()) return

            designation = declarationToResolve.collectDesignationWithFile()
        }

        if (designation.declaration.resolvePhase >= neededPhase) return

        if (neededPhase == FirResolvePhase.IMPORTS) {
            resolveFileToImports(designation.firFile, checkPCE)
            return
        }

        moduleComponents.globalResolveComponents.lockProvider.runCustomResolveUnderLock(designation.firFile, checkPCE) {
            ResolveTreeBuilder.resolveEnsure(designation.declaration, neededPhase) {
                runLazyDesignatedResolveWithoutLock(
                    designation = designation,
                    scopeSession = scopeSession,
                    toPhase = neededPhase,
                    checkPCE = checkPCE,
                )
                designation.declaration
            }
        }
    }


    private fun runLazyDesignatedResolveWithoutLock(
        designation: FirDeclarationDesignationWithFile,
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
            if (checkPCE) checkCanceled()

            LazyTransformerFactory.createLazyTransformer(
                phase = currentPhase,
                designation = designation,
                scopeSession = scopeSession,
                lazyDeclarationResolver = this,
                towerDataContextCollector = null,
                firProviderInterceptor = null,
                checkPCE = checkPCE,
            ).transformDeclaration(moduleComponents.globalResolveComponents.phaseRunner)
        }
    }

    internal fun runLazyDesignatedOnAirResolveToBodyWithoutLock(
        designation: FirDeclarationDesignationWithFile,
        checkPCE: Boolean,
        onAirCreatedDeclaration: Boolean,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        resolveFileToImportsWithoutLock(designation.firFile, checkPCE)
        var currentPhase = maxOf(designation.declaration.resolvePhase, FirResolvePhase.IMPORTS)

        val scopeSession = ScopeSession()

        val firProviderInterceptor = if (onAirCreatedDeclaration) {
            LLFirFirProviderInterceptor.createForFirElement(
                session = designation.firFile.moduleData.session,
                firFile = designation.firFile,
                element = designation.declaration
            )
        } else null

        while (currentPhase < FirResolvePhase.BODY_RESOLVE) {
            currentPhase = currentPhase.next
            if (checkPCE) checkCanceled()

            LazyTransformerFactory.createLazyTransformer(
                phase = currentPhase,
                designation = designation,
                scopeSession = scopeSession,
                lazyDeclarationResolver = this,
                towerDataContextCollector = towerDataContextCollector,
                firProviderInterceptor = firProviderInterceptor,
                checkPCE = checkPCE,
            ).transformDeclaration(moduleComponents.globalResolveComponents.phaseRunner)
        }
    }
}

private fun rethrowWithDetails(
    e: Throwable,
    firDeclarationToResolve: FirDeclaration,
    fromPhase: FirResolvePhase,
    toPhase: FirResolvePhase?
): Nothing {
    if (shouldIjPlatformExceptionBeRethrown(e)) throw e
    buildErrorWithAttachment(
        buildString {
            val moduleData = firDeclarationToResolve.llFirModuleData
            appendLine("Error while resolving ${firDeclarationToResolve::class.java.name} ")
            appendLine("from $fromPhase to $toPhase")
            appendLine("current declaration phase ${firDeclarationToResolve.resolvePhase}")
            appendLine("declaration origin: ${firDeclarationToResolve.origin}")
            appendLine("declaration session: ${firDeclarationToResolve.llFirSession::class}")
            appendLine("declaration module data: ${moduleData::class}")
            appendLine("declaration KtModule: ${moduleData.ktModule::class}")
            appendLine("declaration platform: ${moduleData.ktModule.platform}")
        },
        cause = e,
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


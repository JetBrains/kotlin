/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.api.*
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.*
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.resolvePhaseForAllDeclarations
import org.jetbrains.kotlin.idea.fir.low.level.api.util.*
import org.jetbrains.kotlin.idea.util.ifFalse
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

enum class ResolveType {
    FileAnnotations,
    CallableReturnType,
    ClassSuperTypes,
    DeclarationStatus,
    ValueParametersTypes,
    TypeParametersTypes,
    AnnotationType,
    AnnotationParameters,
    CallableBodyResolve,
    ResolveForMemberScope,
    ResolveForSuperMembers,
    CallableContracts,
    NoResolve,
}

internal class FirLazyDeclarationResolver(
    private val firFileBuilder: FirFileBuilder
) {
    fun lazyResolveDeclaration(
        firDeclaration: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        toResolveType: ResolveType,
        scopeSession: ScopeSession = ScopeSession(),
        checkPCE: Boolean = false,
    ) {
        check(toResolveType == ResolveType.CallableReturnType)
        lazyResolveDeclaration(
            firDeclarationToResolve = firDeclaration,
            moduleFileCache = moduleFileCache,
            toPhase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            scopeSession = scopeSession,
            checkPCE = checkPCE,
        )
    }

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
        runCustomResolveUnderLock(firFile, moduleFileCache, checkPCE) {
            resolveFileAnnotationsWithoutLock(
                firFile = firFile,
                moduleFileCache = moduleFileCache,
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
        moduleFileCache: ModuleFileCache,
        annotations: List<FirAnnotationCall>,
        scopeSession: ScopeSession,
        collector: FirTowerDataContextCollector? = null,
    ) {
        lazyResolveFileDeclarationWithoutLock(
            firFile = firFile,
            moduleFileCache = moduleFileCache,
            toPhase = FirResolvePhase.IMPORTS,
            scopeSession = scopeSession,
            checkPCE = false,
            collector = collector
        )

        FirFileAnnotationsResolveTransformer(
            firFile = firFile,
            annotations = annotations,
            session = firFile.moduleData.session,
            scopeSession = scopeSession,
            firTowerDataContextCollector = collector,
        ).transformDeclaration(firFileBuilder.firPhaseRunner)
    }

    private fun FirDeclaration.isValidForResolve(): Boolean = when (origin) {
        is FirDeclarationOrigin.Source,
        is FirDeclarationOrigin.ImportedFromObject,
        is FirDeclarationOrigin.Delegated,
        is FirDeclarationOrigin.Synthetic,
        is FirDeclarationOrigin.SubstitutionOverride,
        is FirDeclarationOrigin.IntersectionOverride -> {
            when (this) {
                is FirFile -> true
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
        scopeSession: ScopeSession = ScopeSession(),
        checkPCE: Boolean = false,
    ) {
        runCustomResolveUnderLock(firFile, moduleFileCache, checkPCE) {
            lazyResolveFileDeclarationWithoutLock(
                firFile = firFile,
                moduleFileCache = moduleFileCache,
                toPhase = toPhase,
                scopeSession = scopeSession,
                checkPCE = checkPCE,
            )
        }
    }

    private fun lazyResolveFileDeclarationWithoutLock(
        firFile: FirFile,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        checkPCE: Boolean = false,
        collector: FirTowerDataContextCollector? = null,
    ) {
        if (firFile.resolvePhase == FirResolvePhase.RAW_FIR) {
            firFile.transform<FirElement, Any?>(FirImportResolveTransformer(firFile.moduleData.session), null)
            firFile.ensurePhase(FirResolvePhase.IMPORTS)
        }
        if (checkPCE) checkCanceled()
        if (toPhase == FirResolvePhase.IMPORTS) return

        if (toPhase > FirResolvePhase.IMPORTS) {
            resolveFileAnnotations(firFile, firFile.annotations, moduleFileCache, scopeSession, checkPCE, collector)
        }

        for (declaration in firFile.declarations) {
            if (checkPCE) checkCanceled()
            lazyResolveDeclaration(
                firDeclarationToResolve = declaration,
                moduleFileCache = moduleFileCache,
                scopeSession = scopeSession,
                toPhase = toPhase,
                checkPCE = checkPCE
            )
        }
    }

    /**
     * Run designated resolve only designation with fully resolved path (synchronized).
     * Suitable for body resolve or/and on-air resolve.
     * @see lazyResolveDeclaration for ordinary resolve
     * @param firDeclarationToResolve target non-local declaration
     */
    fun lazyResolveDeclaration(
        firDeclarationToResolve: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession = ScopeSession(),
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
        declarationPhaseDowngraded: Boolean = false,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        //TODO Should be synchronised
        if (!firDeclarationToResolve.isValidForResolve()) return

        if (firDeclarationToResolve is FirFile) {
            lazyResolveFileDeclaration(
                firFile = firDeclarationToResolve,
                moduleFileCache = moduleFileCache,
                toPhase = toPhase,
                scopeSession = scopeSession,
                checkPCE = checkPCE,
            )
            return
        }

        val provider = firDeclarationToResolve.moduleData.session.firIdeProvider
        val resolvableDeclaration = firDeclarationToResolve.getNonLocalDeclarationToResolve(provider, moduleFileCache)
        //TODO Should be synchronised
        if (!resolvableDeclaration.isValidForResolve()) return
        val containerFirFile = resolvableDeclaration.getContainingFile()
            ?: error("FirFile was not found for\n${resolvableDeclaration.render()}")

        val designation = resolvableDeclaration.collectDesignation(containerFirFile)
        //TODO Should be synchronised
        val resolvePhase = designation.resolvePhaseForAllDeclarations(includeDeclarationPhase = declarationPhaseDowngraded)
        if (resolvePhase >= toPhase) return

        runCustomResolveUnderLock(containerFirFile, moduleFileCache, checkPCE) {
            runLazyDesignatedResolveWithoutLock(
                designation = designation,
                moduleFileCache = moduleFileCache,
                scopeSession = scopeSession,
                toPhase = toPhase,
                checkPCE = checkPCE,
                declarationPhaseDowngraded = declarationPhaseDowngraded,
            )
        }
    }

    private fun runLazyDesignatedResolveWithoutLock(
        designation: FirDeclarationDesignationWithFile,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
        declarationPhaseDowngraded: Boolean,
    ) {
        val filePhase = designation.firFile.resolvePhase
        if (filePhase == FirResolvePhase.RAW_FIR) {
            lazyResolveFileDeclarationWithoutLock(
                firFile = designation.firFile,
                moduleFileCache = moduleFileCache,
                toPhase = FirResolvePhase.IMPORTS,
                scopeSession = scopeSession,
                checkPCE = checkPCE
            )
        }
        if (toPhase == FirResolvePhase.IMPORTS) return

        val designationPhase = designation.resolvePhaseForAllDeclarations(includeDeclarationPhase = declarationPhaseDowngraded)
        var currentPhase = maxOf(designationPhase, FirResolvePhase.IMPORTS)

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()

            currentPhase.createLazyTransformer(
                designation = designation,
                scopeSession = scopeSession,
                declarationPhaseDowngraded = declarationPhaseDowngraded,
                moduleFileCache = moduleFileCache,
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
        resolveWithUnchangedFir: Boolean,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val scopeSession = ScopeSession()
        var currentPhase = maxOf(designation.declaration.resolvePhase, FirResolvePhase.IMPORTS)

        val firProviderInterceptor = resolveWithUnchangedFir.ifFalse {
            FirProviderInterceptorForIDE.createForFirElement(
                session = designation.firFile.moduleData.session,
                firFile = designation.firFile,
                element = designation.declaration
            )
        }

        while (currentPhase < FirResolvePhase.BODY_RESOLVE) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()

            currentPhase.createLazyTransformer(
                designation = designation,
                scopeSession = scopeSession,
                declarationPhaseDowngraded = true,
                moduleFileCache = moduleFileCache,
                towerDataContextCollector = towerDataContextCollector,
                firProviderInterceptor = firProviderInterceptor,
                checkPCE = checkPCE,
            ).transformDeclaration(firFileBuilder.firPhaseRunner)
        }
    }

    private fun FirResolvePhase.createLazyTransformer(
        designation: FirDeclarationDesignationWithFile,
        scopeSession: ScopeSession,
        declarationPhaseDowngraded: Boolean,
        moduleFileCache: ModuleFileCache,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?,
        checkPCE: Boolean,
    ): FirLazyTransformerForIDE = when (this) {
        FirResolvePhase.SEALED_CLASS_INHERITORS -> FirLazyTransformerForIDE.DUMMY
        FirResolvePhase.SUPER_TYPES -> FirDesignatedSupertypeResolverTransformerForIDE(
            designation = designation,
            session = designation.firFile.moduleData.session,
            scopeSession = scopeSession,
            declarationPhaseDowngraded = declarationPhaseDowngraded,
            moduleFileCache = moduleFileCache,
            firLazyDeclarationResolver = this@FirLazyDeclarationResolver,
            firProviderInterceptor = firProviderInterceptor,
            checkPCE = checkPCE,
        )
        FirResolvePhase.TYPES -> FirDesignatedTypeResolverTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            declarationPhaseDowngraded,
        )
        FirResolvePhase.STATUS -> FirDesignatedStatusResolveTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            declarationPhaseDowngraded,
        )
        FirResolvePhase.CONTRACTS -> FirDesignatedContractsResolveTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            declarationPhaseDowngraded,
        )
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> FirDesignatedImplicitTypesTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            declarationPhaseDowngraded,
            towerDataContextCollector
        )
        FirResolvePhase.BODY_RESOLVE -> FirDesignatedBodyResolveTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            declarationPhaseDowngraded,
            towerDataContextCollector,
            firProviderInterceptor,
        )
        else -> error("Non-lazy phase $this")
    }

    private fun FirDeclaration.getNonLocalDeclarationToResolve(provider: FirProvider, moduleFileCache: ModuleFileCache): FirDeclaration {
        if (this is FirFile) return this

        if (this is FirPropertyAccessor || this is FirTypeParameter || this is FirValueParameter) {
            val ktContainingResolvableDeclaration = when (val psi = this.psi) {
                is KtPropertyAccessor -> psi.property
                is KtProperty -> psi
                is KtParameter, is KtTypeParameter -> psi.getNonLocalContainingOrThisDeclaration()
                    ?: error("Cannot find containing declaration for KtParameter")
                is KtCallExpression -> {
                    check(this.source?.kind == FirFakeSourceElementKind.DefaultAccessor)
                    val delegationCall = psi.parent as KtPropertyDelegate
                    delegationCall.parent as KtProperty
                }
                null -> error("Cannot find containing declaration for KtParameter")
                else -> error("Invalid source of property accessor ${psi::class}")
            }

            val targetElement =
                if (declarationCanBeLazilyResolved(ktContainingResolvableDeclaration)) ktContainingResolvableDeclaration
                else ktContainingResolvableDeclaration.getNonLocalContainingOrThisDeclaration()
            check(targetElement != null) { "Container for local declaration cannot be null" }

            return targetElement.findSourceNonLocalFirDeclaration(
                firFileBuilder = firFileBuilder,
                firSymbolProvider = moduleData.session.symbolProvider,
                moduleFileCache = moduleFileCache
            )
        }

        val ktDeclaration = (psi as? KtDeclaration) ?: run {
            (source as? FirFakeSourceElement<*>).psi?.parentOfType()
        }
        check(ktDeclaration is KtDeclaration) {
            "FirDeclaration should have a PSI of type KtDeclaration"
        }

        if (source !is FirFakeSourceElement<*> && declarationCanBeLazilyResolved(ktDeclaration)) return this
        val nonLocalPsi = ktDeclaration.getNonLocalContainingOrThisDeclaration()
            ?: error("Container for local declaration cannot be null")
        return nonLocalPsi.findSourceNonLocalFirDeclaration(firFileBuilder, provider.symbolProvider, moduleFileCache)
    }

    companion object {
        /**
         * Runs [resolve] function (which is considered to do some resolve on [firFile]) under a lock for [firFile]
         */
        internal inline fun <R> runCustomResolveUnderLock(firFile: FirFile, cache: ModuleFileCache, checkPCE: Boolean, body: () -> R): R {
            return if (checkPCE) {
                cache.firFileLockProvider.withWriteLockPCECheck(firFile, LOCKING_INTERVAL_MS, body)
            } else {
                cache.firFileLockProvider.withWriteLock(firFile, body)
            }
        }

        private const val LOCKING_INTERVAL_MS = 500L

        fun declarationCanBeLazilyResolved(declaration: KtDeclaration): Boolean {
            return when (declaration) {
                !is KtNamedDeclaration -> false
                is KtDestructuringDeclarationEntry, is KtFunctionLiteral, is KtTypeParameter -> false
                is KtPrimaryConstructor -> false
                is KtParameter -> declaration.hasValOrVar() && declaration.containingClassOrObject?.getClassId() != null
                is KtCallableDeclaration, is KtEnumEntry -> {
                    when (val parent = declaration.parent) {
                        is KtFile -> true
                        is KtClassBody -> (parent.parent as? KtClassOrObject)?.getClassId() != null
                        else -> false
                    }
                }
                is KtClassLikeDeclaration -> declaration.getClassId() != null
                else -> error("Unexpected ${declaration::class.qualifiedName}")
            }
        }
    }
}

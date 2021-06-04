/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver

internal object LazyTransformerFactory {
    fun createLazyTransformer(
        phase: FirResolvePhase,
        designation: FirDeclarationDesignationWithFile,
        scopeSession: ScopeSession,
        declarationPhaseDowngraded: Boolean,
        moduleFileCache: ModuleFileCache,
        lazyDeclarationResolver: FirLazyDeclarationResolver,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?,
        checkPCE: Boolean,
    ): FirLazyTransformerForIDE = when (phase) {
        FirResolvePhase.SEALED_CLASS_INHERITORS -> FirLazyTransformerForIDE.DUMMY
        FirResolvePhase.SUPER_TYPES -> FirDesignatedSupertypeResolverTransformerForIDE(
            designation = designation,
            session = designation.firFile.moduleData.session,
            scopeSession = scopeSession,
            declarationPhaseDowngraded = declarationPhaseDowngraded,
            moduleFileCache = moduleFileCache,
            firLazyDeclarationResolver = lazyDeclarationResolver,
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
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveTransformer


internal object LLFirDesignatedContractsResolvePhaseResolver : LLFirLazyPhaseResolver() {
    override fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        val resolver = LLFirContractsResolver(designation, lockProvider, session, scopeSession)
        resolver.resolve()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, FirResolvePhase.CONTRACTS, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.CONTRACTS)
        if (target is FirContractDescriptionOwner) {
            // TODO checkContractDescriptionIsResolved(declaration)
        }
        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirContractsResolver(
    designation: LLFirDesignationToResolve,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirAbstractBodyResolver(
    designation,
    lockProvider,
    FirResolvePhase.CONTRACTS
) {
    override val transformer = FirContractResolveTransformer(session, scopeSession)

    override fun resolveDeclarationContent(target: FirElementWithResolveState) {
        when (target) {
            is FirRegularClass, is FirAnonymousInitializer, is FirDanglingModifierList, is FirFileAnnotationsContainer, is FirTypeAlias -> {
                // no contracts here
            }
            is FirCallableDeclaration -> {
                // TODO calculate bodies only when in-body contract is present
                calculateLazyBodies(target)
                target.transform<FirDeclaration, _>(transformer, ResolutionMode.ContextIndependent)
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }
}

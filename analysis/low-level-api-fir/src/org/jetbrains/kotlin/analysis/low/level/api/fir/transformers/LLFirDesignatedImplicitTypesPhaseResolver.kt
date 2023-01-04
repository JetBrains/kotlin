/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.LLFirDesignatedImpliciteTypesBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkReturnTypeRefIsResolved
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitAwareBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE

internal object LLFirDesignatedImplicitTypesPhaseResolver : LLFirLazyPhaseResolver() {
    override fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        val resolver = LLFirImplicitBodyResolver(designation, lockProvider, session, scopeSession, towerDataContextCollector)
        resolver.resolve()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(
            target,
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            updateForLocalDeclarations = false/* here should be true if we resolved the body*/
        )
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        if (target is FirCallableDeclaration) {
            checkReturnTypeRefIsResolved(target)
        }
        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirImplicitBodyResolver(
    designation: LLFirDesignationToResolve,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    towerDataContextCollector: FirTowerDataContextCollector?,
) : LLFirAbstractBodyResolver(
    designation,
    lockProvider,
    FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
) {
    private val implicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession()

    override val transformer = FirImplicitAwareBodyResolveTransformer(
        session,
        implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
        phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
        implicitTypeOnly = true,
        scopeSession = scopeSession,
        firTowerDataContextCollector = towerDataContextCollector,
        returnTypeCalculator = createReturnTypeCalculatorForIDE(
            scopeSession,
            implicitBodyResolveComputationSession,
            ::LLFirDesignatedImpliciteTypesBodyResolveTransformerForReturnTypeCalculator
        )
    )

    override fun resolveDeclarationContent(target: FirElementWithResolveState) {
        when (target) {
            is FirRegularClass, is FirDanglingModifierList, is FirAnonymousInitializer, is FirFileAnnotationsContainer, is FirTypeAlias -> {
                // no implicit bodies here
            }
            is FirCallableDeclaration -> {
                calculateLazyBodies(target)
                target.transform<FirDeclaration, _>(transformer, ResolutionMode.ContextIndependent)
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }
}


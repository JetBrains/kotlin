/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
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
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitAwareBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.scopes.fakeOverrideSubstitution
import org.jetbrains.kotlin.fir.visitors.transformSingle

internal object LLFirImplicitTypesLazyResolver : LLFirLazyResolver(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val resolver = LLFirImplicitBodyTargetResolver(target, lockProvider, session, scopeSession, towerDataContextCollector)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(
            target,
            resolverPhase,
            updateForLocalDeclarations = false/* here should be true if we resolved the body*/
        )
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(resolverPhase)
        if (target is FirCallableDeclaration) {
            checkReturnTypeRefIsResolved(target)
        }

        checkNestedDeclarationsAreResolved(target)
    }
}

internal class LLFirImplicitBodyTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    towerDataContextCollector: FirTowerDataContextCollector?,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession? = null,
) : LLFirAbstractBodyTargetResolver(
    target,
    lockProvider,
    scopeSession,
    FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitBodyResolveComputationSession = implicitBodyResolveComputationSession ?: ImplicitBodyResolveComputationSession(),
    isJumpingPhase = true,
) {
    override val transformer = object : FirImplicitAwareBodyResolveTransformer(
        session,
        implicitBodyResolveComputationSession = this.implicitBodyResolveComputationSession,
        phase = resolverPhase,
        implicitTypeOnly = true,
        scopeSession = scopeSession,
        firTowerDataContextCollector = towerDataContextCollector,
        returnTypeCalculator = createReturnTypeCalculator(towerDataContextCollector = towerDataContextCollector),
    ) {
        override val preserveCFGForClasses: Boolean get() = false
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        when (target) {
            is FirRegularClass,
            is FirDanglingModifierList,
            is FirAnonymousInitializer,
            is FirFileAnnotationsContainer,
            is FirTypeAlias,
            is FirScript,
            is FirConstructor,
            -> {
                // no implicit bodies here
            }

            is FirCallableDeclaration -> {
                if (target.attributes.fakeOverrideSubstitution != null) {
                    transformer.returnTypeCalculator.fakeOverrideTypeCalculator.computeReturnType(target)
                } else {
                    calculateLazyBodies(target)
                    target.transformSingle(transformer, ResolutionMode.ContextIndependent)
                }
            }

            else -> throwUnexpectedFirElementError(target)
        }
    }
}


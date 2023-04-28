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
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitAwareBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

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

private class LLFirImplicitBodyTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    towerDataContextCollector: FirTowerDataContextCollector?,
) : LLFirAbstractBodyTargetResolver(
    target,
    lockProvider,
    scopeSession,
    FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
) {
    override val transformer = object : FirImplicitAwareBodyResolveTransformer(
        session,
        implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
        phase = resolverPhase,
        implicitTypeOnly = true,
        scopeSession = scopeSession,
        firTowerDataContextCollector = towerDataContextCollector,
        returnTypeCalculator = createReturnTypeCalculator(),
    ) {
        override val preserveCFGForClasses: Boolean get() = false
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        when (target) {
            is FirSimpleFunction -> resolve(target, ImplicitTypeBodyStateKeepers.FUNCTION)
            is FirProperty -> resolve(target, ImplicitTypeBodyStateKeepers.PROPERTY)
            is FirPropertyAccessor -> resolve(target.propertySymbol.fir, ImplicitTypeBodyStateKeepers.PROPERTY)
            is FirRegularClass,
            is FirDanglingModifierList,
            is FirAnonymousInitializer,
            is FirFileAnnotationsContainer,
            is FirTypeAlias,
            is FirConstructor,
            is FirEnumEntry,
            is FirScript,
            is FirCallableDeclaration -> {
                // No implicit bodies here
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }
}

internal object ImplicitTypeBodyStateKeepers {
    val FUNCTION: StateKeeper<FirFunction> = stateKeeper {
        add(BodyStateKeepers.FUNCTION)
        add(FirFunction::returnTypeRef, FirFunction::replaceReturnTypeRef)
    }

    val PROPERTY: StateKeeper<FirProperty> = stateKeeper {
        add(BodyStateKeepers.PROPERTY)
        add(FirProperty::returnTypeRef, FirProperty::replaceReturnTypeRef)
    }
}
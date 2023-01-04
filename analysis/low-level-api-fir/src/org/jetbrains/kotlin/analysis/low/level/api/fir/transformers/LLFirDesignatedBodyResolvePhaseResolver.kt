/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.LLFirEnsureBasedTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE


internal object LLFirDesignatedBodyResolvePhaseResolver : LLFirLazyPhaseResolver() {

    override fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        val resolver =
            LLFirBodyResolver(designation, lockProvider, session, scopeSession, towerDataContextCollector, firProviderInterceptor)
        resolver.resolve()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, FirResolvePhase.BODY_RESOLVE, updateForLocalDeclarations = true)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.BODY_RESOLVE)
        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirBodyResolver(
    designation: LLFirDesignationToResolve,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    towerDataContextCollector: FirTowerDataContextCollector?,
    firProviderInterceptor: FirProviderInterceptor?,
) : LLFirAbstractBodyResolver(
    designation,
    lockProvider,
    FirResolvePhase.BODY_RESOLVE,
) {
    override val transformer = object : FirBodyResolveTransformer(
        session,
        phase = FirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
        returnTypeCalculator = createReturnTypeCalculatorForIDE(
            scopeSession,
            ImplicitBodyResolveComputationSession(),
            ::LLFirEnsureBasedTransformerForReturnTypeCalculator
        ),
        firTowerDataContextCollector = towerDataContextCollector,
        firProviderInterceptor = firProviderInterceptor,
    ) {
        override val preserveCFGForClasses: Boolean get() = false
    }

    override fun resolveWithoutLock(target: FirElementWithResolveState): Boolean {
        when (target) {
            is FirRegularClass -> {
                if (target.resolveState.resolvePhase >= FirResolvePhase.BODY_RESOLVE) return true

                // resolve class CFG graph here, to do this we need to have property & init blocks resoled
                resolveMemberProperties(target)
                resolve(target) {
                    calculateCFG(target)
                }

                return true
            }
        }
        return false
    }

    private fun calculateCFG(target: FirRegularClass) {
        checkWithAttachmentBuilder(
            target.controlFlowGraphReference == null,
            { "controlFlowGraphReference should be null if class phase < FirResolvePhase.BODY_RESOLVE)" },
        ) {
            withFirEntry("firClass", target)
        }
        val dataFlowAnalyzer = transformer.declarationsTransformer.dataFlowAnalyzer
        dataFlowAnalyzer.enterClass()
        val controlFlowGraph = dataFlowAnalyzer.exitRegularClass(target)
        target.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
    }

    private fun resolveMemberProperties(target: FirRegularClass) {
        withRegularClass(target) {
            for (member in target.declarations) {
                if (member is FirProperty || member is FirAnonymousInitializer /*property may be initialized in a FirAnonymousInitializer */) {
                    resolveTarget(member)
                }
            }
        }
    }

    override fun resolveDeclarationContent(target: FirElementWithResolveState) {
        when (target) {
            is FirRegularClass -> {
                error("should be resolved in ${::resolveWithoutLock.name}")
            }
            is FirDanglingModifierList, is FirFileAnnotationsContainer, is FirTypeAlias -> {
                // no bodies here
            }
            is FirCallableDeclaration, is FirAnonymousInitializer -> {
                calculateLazyBodies(target)
                target.transform<FirDeclaration, _>(transformer, ResolutionMode.ContextIndependent)
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }
}


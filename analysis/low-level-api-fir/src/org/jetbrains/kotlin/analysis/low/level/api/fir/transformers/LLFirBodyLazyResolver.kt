/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDelegatedConstructorIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkBodyIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDefaultValueIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkInitializerIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.transformSingle


internal object LLFirBodyLazyResolver : LLFirLazyResolver(FirResolvePhase.BODY_RESOLVE) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val resolver = LLFirBodyTargetResolver(target, lockProvider, session, scopeSession, towerDataContextCollector)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = true)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(resolverPhase)
        when (target) {
            is FirValueParameter -> checkDefaultValueIsResolved(target)
            is FirVariable -> checkInitializerIsResolved(target)
            is FirConstructor -> {
                checkDelegatedConstructorIsResolved(target)
                checkBodyIsResolved(target)
            }
            is FirFunction -> checkBodyIsResolved(target)
        }

        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirBodyTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    towerDataContextCollector: FirTowerDataContextCollector?,
) : LLFirAbstractBodyTargetResolver(
    target,
    lockProvider,
    scopeSession,
    FirResolvePhase.BODY_RESOLVE,
) {
    override val transformer = object : FirBodyResolveTransformer(
        session,
        phase = resolverPhase,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
        returnTypeCalculator = createReturnTypeCalculator(towerDataContextCollector = towerDataContextCollector),
        firTowerDataContextCollector = towerDataContextCollector,
    ) {
        override val preserveCFGForClasses: Boolean get() = false
    }

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        when (target) {
            is FirRegularClass -> {
                if (target.resolvePhase >= resolverPhase) return true

                // resolve class CFG graph here, to do this we need to have property & init blocks resoled
                resolveMemberProperties(target)
                performCustomResolveUnderLock(target) {
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
            { "controlFlowGraphReference should be null if class phase < $resolverPhase)" },
        ) {
            withFirEntry("firClass", target)
        }

        val dataFlowAnalyzer = transformer.declarationsTransformer.dataFlowAnalyzer
        dataFlowAnalyzer.enterClass(target, buildGraph = true)
        val controlFlowGraph = dataFlowAnalyzer.exitClass()
            ?: buildErrorWithAttachment("CFG should not be null as buildGraph is specified") {
                withFirEntry("firClass", target)
            }

        target.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
    }

    private fun resolveMemberProperties(target: FirRegularClass) {
        withRegularClass(target) {
            transformer.firTowerDataContextCollector?.addDeclarationContext(target, transformer.context.towerDataContext)

            for (member in target.declarations) {
                if (member is FirCallableDeclaration || member is FirAnonymousInitializer) {
                    /* TODO we should resolve only properties and init blocks here but due to the recent changes in the compiler, we also have to do this for all callable members
                    we should avoid doing it as it leads to additional work and also can might to problems with incremental analysis
                    */
                    member.lazyResolveToPhase(resolverPhase.previous)
                    performResolve(member)
                }
            }
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        val contextCollector = transformer.firTowerDataContextCollector
        if (contextCollector != null && target is FirDeclaration) {
            val bodyResolveContext = transformer.context
            if (target is FirFunction) {
                bodyResolveContext.forFunctionBody(target, transformer.components) {
                    contextCollector.addDeclarationContext(target, bodyResolveContext.towerDataContext)
                }
            } else {
                contextCollector.addDeclarationContext(target, bodyResolveContext.towerDataContext)
            }
        }

        when (target) {
            is FirRegularClass -> {
                error("should be resolved in ${::doResolveWithoutLock.name}")
            }
            is FirDanglingModifierList, is FirFileAnnotationsContainer, is FirTypeAlias -> {
                // no bodies here
            }
            is FirCallableDeclaration, is FirAnonymousInitializer, is FirScript -> {
                calculateLazyBodies(target)
                target.transformSingle(transformer, ResolutionMode.ContextIndependent)
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }
}


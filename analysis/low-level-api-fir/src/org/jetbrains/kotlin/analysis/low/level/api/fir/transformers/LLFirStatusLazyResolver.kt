/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDeclarationStatusIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
import org.jetbrains.kotlin.fir.visitors.transformSingle

internal object LLFirStatusLazyResolver : LLFirLazyResolver(FirResolvePhase.STATUS) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val resolver = LLFirStatusTargetResolver(target, lockProvider, session, scopeSession)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        if (target !is FirAnonymousInitializer) {
            target.checkPhase(resolverPhase)
        }
        if (target is FirMemberDeclaration) {
            checkDeclarationStatusIsResolved(target)
        }
        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirStatusTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    private val statusComputationSession: StatusComputationSession = StatusComputationSession()
) : LLFirTargetResolver(target, lockProvider, FirResolvePhase.STATUS, isJumpingPhase = true) {
    private val transformer = Transformer(session, scopeSession)

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        firClass.lazyResolveToPhase(resolverPhase.previous)
        resolveClass(firClass, action)
    }

    private fun resolveClassTypeParameters(klass: FirClass) {
        klass.typeParameters.forEach { it.transformSingle(transformer, data = null) }
    }

    private fun resolveCallableMembers(klass: FirClass) {
        // we need the types to be resolved here to compute visibility
        // implicit types will not be handled (see bug in the compiler KT-55446)
        klass.lazyResolveToPhaseWithCallableMembers(resolverPhase.previous)
        for (member in klass.declarations) {
            if (member !is FirCallableDeclaration) continue
            performResolve(member)
        }
    }

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        return when (target) {
            is FirRegularClass -> {
                resolveClass(target, action = {})
                true
            }
            else -> {
                false
            }
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        when (target) {
            is FirRegularClass -> {
                error("should be resolved in doResolveWithoutLock")
            }
            else -> {
                target.transformSingle(transformer, data = null)
            }
        }
    }

    private fun resolveClass(firClass: FirRegularClass, action: () -> Unit) {
        transformer.statusComputationSession.startComputing(firClass)
        transformer.forceResolveStatusesOfSupertypes(firClass)

        performCustomResolveUnderLock(firClass) {
            transformer.transformClassStatus(firClass)
            transformer.transformValueClassRepresentation(firClass)
            transformer.storeClass(firClass) {
                resolveClassTypeParameters(firClass)
                firClass
            }
        }

        transformer.storeClass(firClass) {
            resolveCallableMembers(firClass)
            firClass
        }

        transformer.storeClass(firClass) {
            action()
            firClass
        }

        transformer.statusComputationSession.endComputing(firClass)
    }


    private inner class Transformer(
        session: FirSession,
        scopeSession: ScopeSession,
    ) : FirStatusResolveTransformer(session, scopeSession, statusComputationSession) {

        override fun FirDeclaration.needResolveMembers(): Boolean = false
        override fun FirDeclaration.needResolveNestedClassifiers(): Boolean = false

        override fun transformClass(klass: FirClass, data: FirResolvedDeclarationStatus?): FirStatement {
            return klass
        }

        override fun resolveClassForSuperType(regularClass: FirRegularClass): Boolean {
            if (regularClass.resolvePhase >= resolverPhase) return true
            regularClass.lazyResolveToPhase(resolverPhase.previous)

            val designation = regularClass.collectDesignationWithFile().asResolveTarget()
            val resolver = LLFirStatusTargetResolver(designation, lockProvider, session, scopeSession, statusComputationSession)
            resolver.resolveDesignation()
            return true
        }
    }
}

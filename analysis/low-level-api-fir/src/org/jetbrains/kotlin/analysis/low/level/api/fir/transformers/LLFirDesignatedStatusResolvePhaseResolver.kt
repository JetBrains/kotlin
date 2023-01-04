/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDeclarationStatusIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.transformSingle

internal object LLFirDesignatedStatusResolvePhaseResolver : LLFirLazyPhaseResolver() {
    override fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        val resolver = LLFirStatusResolver(designation, lockProvider, session, scopeSession)
        resolver.resolve()
    }


    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, FirResolvePhase.STATUS, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        if (target !is FirAnonymousInitializer) {
            target.checkPhase(FirResolvePhase.STATUS)
        }
        if (target is FirMemberDeclaration) {
            checkDeclarationStatusIsResolved(target)
        }
        checkNestedDeclarationsAreResolved(target)
    }
}


private class LLFirStatusResolver(
    designation: LLFirDesignationToResolve,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirAbstractMultiDesignationResolver(designation, lockProvider, FirResolvePhase.STATUS) {

    private val transformer = Transformer(session, scopeSession)

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    override fun withRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        val computationStatus = transformer.statusComputationSession.startComputing(firClass)
        transformer.forceResolveStatusesOfSupertypes(firClass)
        /*
         * Status of class may be already calculated if that class was in supertypes of one of previous classes
         */
        if (computationStatus != StatusComputationSession.StatusComputationStatus.Computed) {
            transformer.transformClassStatus(firClass)
            transformer.trasnformValueClassRepresentation(firClass)
        }

        transformer.storeClass(firClass) {
            // resolveClassTypeParameters TODO should happen under the lock
            resolveClassTypeParameters(firClass)
            resolveCallableMembers(firClass)
            resolveTarget(firClass)
            action()
            firClass
        }
        transformer.statusComputationSession.endComputing(firClass)
    }

    private fun resolveClassTypeParameters(klass: FirClass) {
        klass.typeParameters.forEach { it.transformSingle(transformer, data = null) }
    }

    private fun resolveCallableMembers(klass: FirClass) {
        for (member in klass.declarations) {
            if (member is FirClassLikeDeclaration) continue
            // we need the types to be resolved here to compute visibility
            // implicit types will not be handled (see bug in the compiler KT-55446)
            member.lazyResolveToPhase(FirResolvePhase.TYPES)
            resolveTarget(member)
        }
    }

    override fun resolveDeclarationContent(target: FirElementWithResolveState) {
        target.transformSingle(transformer, data = null)
    }

    private inner class Transformer(
        session: FirSession,
        scopeSession: ScopeSession,
    ) : FirStatusResolveTransformer(session, scopeSession, StatusComputationSession()) {

        override fun FirDeclaration.needResolveMembers(): Boolean = false
        override fun FirDeclaration.needResolveNestedClassifiers(): Boolean = false

        override fun transformClass(klass: FirClass, data: FirResolvedDeclarationStatus?): FirStatement {
            return klass
        }
    }
}


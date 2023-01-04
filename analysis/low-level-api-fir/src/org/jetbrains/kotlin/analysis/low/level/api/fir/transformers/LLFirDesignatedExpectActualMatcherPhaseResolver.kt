/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatcherTransformer

internal object LLFirDesignatedExpectActualMatcherPhaseResolver : LLFirLazyPhaseResolver() {

    override fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        val resolver = LLFirExpectActualMatchingResolver(designation, lockProvider, session, scopeSession)
        resolver.resolve()
    }


    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(
            target,
            FirResolvePhase.EXPECT_ACTUAL_MATCHING,
            updateForLocalDeclarations = false
        )
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
        // TODO check if expect-actual matching is present
        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirExpectActualMatchingResolver(
    designation: LLFirDesignationToResolve,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirAbstractMultiDesignationResolver(designation, lockProvider, FirResolvePhase.EXPECT_ACTUAL_MATCHING) {
    private val transformer = FirExpectActualMatcherTransformer(session, scopeSession)

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    override fun withRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        action()
    }

    override fun resolveDeclarationContent(target: FirElementWithResolveState) {
        if (target !is FirMemberDeclaration) return
        if (!shouldTransform(target)) return
        transformer.transformMemberDeclaration(target)
    }

    private fun shouldTransform(target: FirMemberDeclaration) = when (target) {
        is FirEnumEntry -> true
        is FirProperty -> true
        is FirConstructor -> true
        is FirSimpleFunction -> true
        is FirRegularClass -> true
        is FirTypeAlias -> true
        else -> false
    }
}
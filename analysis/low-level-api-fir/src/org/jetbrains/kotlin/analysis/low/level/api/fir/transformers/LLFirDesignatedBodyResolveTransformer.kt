/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.LLFirEnsureBasedTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState

/**
 * Transform designation into BODY_RESOLVE declaration. Affects only for target declaration and it's children
 */
internal class LLFirDesignatedBodyResolveTransformer(
    private val designation: FirDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
    towerDataContextCollector: FirTowerDataContextCollector?
) : LLFirLazyTransformer, FirBodyResolveTransformer(
    session,
    phase = FirResolvePhase.BODY_RESOLVE,
    implicitTypeOnly = false,
    scopeSession = scopeSession,
    returnTypeCalculator = createReturnTypeCalculatorForIDE(
        scopeSession,
        ImplicitBodyResolveComputationSession(),
        ::LLFirEnsureBasedTransformerForReturnTypeCalculator
    ),
    firTowerDataContextCollector = towerDataContextCollector
) {
    private val ideDeclarationTransformer = LLFirDeclarationTransformer(designation)

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            super.transformDeclarationContent(declaration, data)
        }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.target.resolvePhase >= FirResolvePhase.BODY_RESOLVE) return
        designation.target.checkPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.BODY_RESOLVE) {
            designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
        }


        ideDeclarationTransformer.ensureDesignationPassed()
        updatePhaseDeep(designation.target, FirResolvePhase.BODY_RESOLVE, withNonLocalDeclarations = true)
        checkIsResolved(designation.target)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.BODY_RESOLVE)
        checkNestedDeclarationsAreResolved(target)
    }
}


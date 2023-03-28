/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirAbstractContractResolveTransformerDispatcher

/**
 * Transform designation into CONTRACTS declaration. Affects only for target declaration and it's children
 */
internal class LLFirDesignatedContractsResolveTransformer(
    private val designation: FirDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirLazyTransformer, FirAbstractContractResolveTransformerDispatcher(session, scopeSession) {

    private val ideDeclarationTransformer = LLFirDeclarationTransformer(designation)

    override val contractDeclarationsTransformer: FirDeclarationsContractResolveTransformer
        get() = object : FirDeclarationsContractResolveTransformer() {
            override fun transformDeclarationContent(firClass: FirClass, data: ResolutionMode) {
                ideDeclarationTransformer.transformDeclarationContent(this, firClass, data) {
                    super.transformDeclarationContent(firClass, data)
                    firClass
                }
            }
        }

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            super.transformDeclarationContent(declaration, data)
        }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.target.resolvePhase >= FirResolvePhase.CONTRACTS) return
        designation.target.checkPhase(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)

        FirLazyBodiesCalculator.calculateLazyBodiesInside(designation)
        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.CONTRACTS) {
            designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
        }

        ideDeclarationTransformer.ensureDesignationPassed()
        updatePhaseDeep(designation.target, FirResolvePhase.CONTRACTS)
        checkIsResolved(designation.target)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.CONTRACTS)
        if (target is FirContractDescriptionOwner) {
            // TODO checkContractDescriptionIsResolved(declaration)
        }
        checkNestedDeclarationsAreResolved(target)
    }
}

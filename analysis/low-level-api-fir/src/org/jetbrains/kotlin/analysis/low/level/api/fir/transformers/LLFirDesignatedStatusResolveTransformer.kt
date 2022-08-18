/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StatusComputationSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDeclarationStatusIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase

/**
 * Transform designation into STATUS phase. Affects only for designation, target declaration, it's children and dependents
 */
internal class LLFirDesignatedStatusResolveTransformer(
    private val designation: FirDeclarationDesignationWithFile,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
) : LLFirLazyTransformer {
    private inner class FirDesignatedStatusResolveTransformerForIDE :
        FirStatusResolveTransformer(session, scopeSession, StatusComputationSession.Regular()) {

        val designationTransformer = LLFirDeclarationTransformer(designation)

        override fun transformDeclarationContent(declaration: FirDeclaration, data: FirResolvedDeclarationStatus?): FirDeclaration =
            designationTransformer.transformDeclarationContent(this, declaration, data) {
                super.transformDeclarationContent(declaration, data)
            }
    }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.STATUS) return
        designation.declaration.checkPhase(FirResolvePhase.TYPES)

        val transformer = FirDesignatedStatusResolveTransformerForIDE()
        ResolveTreeBuilder.resolvePhase(designation.declaration, FirResolvePhase.STATUS) {
            phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.STATUS) {
                designation.firFile.transform<FirElement, FirResolvedDeclarationStatus?>(transformer, null)
            }
        }

        transformer.designationTransformer.ensureDesignationPassed()
        updatePhaseDeep(designation.declaration, FirResolvePhase.STATUS)
        checkIsResolved(designation.declaration)
    }

    override fun checkIsResolved(declaration: FirDeclaration) {
        if (declaration !is FirAnonymousInitializer) {
            declaration.checkPhase(FirResolvePhase.STATUS)
        }
        if (declaration is FirMemberDeclaration) {
            checkDeclarationStatusIsResolved(declaration)
        }
        checkNestedDeclarationsAreResolved(declaration)
    }
}

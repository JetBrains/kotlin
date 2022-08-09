/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitAwareBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.LLFirDesignatedImpliciteTypesBodyResolveTransformerForReturnTypeCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkReturnTypeRefIsResolved

/**
 * Transform designation into IMPLICIT_TYPES_BODY_RESOLVE declaration. Affects only for target declaration, it's children and dependents
 */
internal class LLFirDesignatedImplicitTypesTransformer(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
    towerDataContextCollector: FirTowerDataContextCollector?,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession(),
) : LLFirLazyTransformer, FirImplicitAwareBodyResolveTransformer(
    session,
    implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
    phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitTypeOnly = true,
    scopeSession = scopeSession,
    firTowerDataContextCollector = towerDataContextCollector,
    returnTypeCalculator = createReturnTypeCalculatorForIDE(
        scopeSession,
        implicitBodyResolveComputationSession,
        ::LLFirDesignatedImpliciteTypesBodyResolveTransformerForReturnTypeCalculator
    )
) {
    private val ideDeclarationTransformer = LLFirDeclarationTransformer(designation)

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            super.transformDeclarationContent(declaration, data)
        }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) return
        designation.declaration.checkPhase(FirResolvePhase.CONTRACTS)

        ResolveTreeBuilder.resolvePhase(designation.declaration, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
            phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
                designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
            }
        }

        ideDeclarationTransformer.ensureDesignationPassed()
        updatePhaseDeep(designation.declaration, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        checkIsResolved(designation.declaration)
    }

    override fun checkIsResolved(declaration: FirDeclaration) {
        declaration.checkPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        if (declaration is FirCallableDeclaration) {
            checkReturnTypeRefIsResolved(declaration)
        }
        checkNestedDeclarationsAreResolved(declaration)
    }
}

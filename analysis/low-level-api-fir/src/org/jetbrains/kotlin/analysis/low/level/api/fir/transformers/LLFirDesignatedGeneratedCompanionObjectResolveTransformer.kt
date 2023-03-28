/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompanionGenerationTransformer
import org.jetbrains.kotlin.fir.declarations.resolvePhase

internal class LLFirDesignatedGeneratedCompanionObjectResolveTransformer(
    val designation: FirDesignationWithFile,
    session: FirSession
) : LLFirLazyTransformer {
    private val transformer: FirCompanionGenerationTransformer = FirCompanionGenerationTransformer(session)

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.target.resolvePhase >= FirResolvePhase.COMPANION_GENERATION) return

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.COMPANION_GENERATION) {
            designation.target.transform<FirDeclaration, Nothing?>(transformer, null)
        }

        LLFirLazyTransformer.updatePhaseDeep(designation.target, FirResolvePhase.COMPANION_GENERATION)
        checkIsResolved(designation.target)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        check(target.resolvePhase >= FirResolvePhase.COMPANION_GENERATION)
    }
}
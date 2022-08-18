/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompanionGenerationTransformer

internal class LLFirDesignatedGeneratedCompanionObjectResolveTransformer(
    val designation: FirDeclarationDesignationWithFile,
    session: FirSession
) : LLFirLazyTransformer {
    private val transformer: FirCompanionGenerationTransformer = FirCompanionGenerationTransformer(session)

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.COMPANION_GENERATION) return

        ResolveTreeBuilder.resolvePhase(designation.declaration, FirResolvePhase.COMPANION_GENERATION) {
            phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.COMPANION_GENERATION) {
                designation.declaration.transform<FirDeclaration, Nothing?>(transformer, null)
            }
        }

        LLFirLazyTransformer.updatePhaseDeep(designation.declaration, FirResolvePhase.COMPANION_GENERATION)
        checkIsResolved(designation.declaration)
    }

    override fun checkIsResolved(declaration: FirDeclaration) {
        check(declaration.resolvePhase >= FirResolvePhase.COMPANION_GENERATION)
    }
}
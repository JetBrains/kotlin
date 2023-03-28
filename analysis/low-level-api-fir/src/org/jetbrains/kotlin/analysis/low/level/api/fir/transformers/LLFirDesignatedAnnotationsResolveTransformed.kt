/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompilerRequiredAnnotationsResolveTransformer

internal class LLFirDesignatedAnnotationsResolveTransformed(
    private val designation: FirDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirLazyTransformer,
    FirCompilerRequiredAnnotationsResolveTransformer(session, scopeSession, CompilerRequiredAnnotationsComputationSession()) {

    private fun moveNextDeclaration(designationIterator: Iterator<FirElementWithResolveState>) {
        if (!designationIterator.hasNext()) {
            val declaration = designation.target
            FirLazyBodiesCalculator.calculateCompilerAnnotations(declaration)
            if (declaration is FirRegularClass || declaration is FirTypeAlias) {
                declaration.transform<FirDeclaration, Nothing?>(this, null)
            }
            return
        }
        when (val nextElement = designationIterator.next()) {
            is FirFile -> {
                withFileAndScopes(nextElement) {
                    moveNextDeclaration(designationIterator)
                }
            }
            is FirRegularClass -> {
                moveNextDeclaration(designationIterator)
            }
            else -> {
                error("Unexpected declaration in designation: ${nextElement::class.qualifiedName}")
            }
        }
    }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.target.resolvePhase >= FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) return

        val designationIterator = designation.toSequenceWithFile(includeTarget = false).iterator()

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
            moveNextDeclaration(designationIterator)
        }

        LLFirLazyTransformer.updatePhaseDeep(designation.target, FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
        checkIsResolved(designation.target)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
        // todo add proper check that COMPILER_REQUIRED_ANNOTATIONS are resolved
//        checkNestedDeclarationsAreResolved(declaration)
    }
}

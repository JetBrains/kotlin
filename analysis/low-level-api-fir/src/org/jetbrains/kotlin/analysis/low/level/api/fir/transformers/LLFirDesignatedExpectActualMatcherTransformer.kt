/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatcherTransformer

internal class LLFirDesignatedExpectActualMatcherTransformer(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirLazyTransformer, FirExpectActualMatcherTransformer(session, scopeSession) {

    private val declarationTransformer = LLFirDeclarationTransformer(designation)

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        return declarationTransformer.transformDeclarationContent(this, regularClass, data) {
            super.transformRegularClass(regularClass, data) as FirDeclaration
        } as FirStatement
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        return declarationTransformer.transformDeclarationContent(this, file, data) {
            super.transformFile(file, data)
        } as FirFile
    }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.EXPECT_ACTUAL_MATCHING) return
        designation.declaration.checkPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)

        FirLazyBodiesCalculator.calculateLazyBodiesInside(designation)
        ResolveTreeBuilder.resolvePhase(designation.declaration, FirResolvePhase.EXPECT_ACTUAL_MATCHING) {
            phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.EXPECT_ACTUAL_MATCHING) {
                designation.firFile.transform<FirFile, Nothing?>(this, null)
            }
        }

        declarationTransformer.ensureDesignationPassed()
        updatePhaseDeep(designation.declaration, FirResolvePhase.EXPECT_ACTUAL_MATCHING)
        checkIsResolved(designation.declaration)
    }

    override fun checkIsResolved(declaration: FirDeclaration) {
        declaration.checkPhase(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
        // TODO check if expect-actual matching is present
        checkNestedDeclarationsAreResolved(declaration)
    }
}


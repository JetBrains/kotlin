/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkAnnotationArgumentsMappingIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsMappingTransformer
import org.jetbrains.kotlin.fir.declarations.resolvePhase

/**
 * Transform designation into ANNOTATIONS_ARGUMENTS_MAPPING declaration. Affects only for target declaration, it's children and dependents
 */
internal class LLFirDesignatedAnnotationArgumentsMappingTransformer(
    private val designation: FirDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirLazyTransformer, FirAnnotationArgumentsMappingTransformer(session, scopeSession, FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) {
    private val ideDeclarationTransformer = LLFirDeclarationTransformer(designation)

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            super.transformDeclarationContent(declaration, data)
        }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.target.resolvePhase >= FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) return
        designation.target.checkPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) {
            FirLazyBodiesCalculator.calculateAnnotations(designation.firFile)
            designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
        }

        updatePhaseDeep(designation.target, FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)
        checkIsResolved(designation.target)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)
        if (target !is FirAnnotationContainer) return
        for (annotation in target.annotations) {
            if (annotation is FirAnnotationCall) {
                checkAnnotationArgumentsMappingIsResolved(annotation, target)
            }
        }
        checkNestedDeclarationsAreResolved(target)
    }
}

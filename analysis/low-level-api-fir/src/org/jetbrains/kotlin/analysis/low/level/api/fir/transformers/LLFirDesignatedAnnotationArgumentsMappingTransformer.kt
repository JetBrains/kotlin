/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirLazyTransformer.Companion.updatePhaseDeep
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkAnnotationArgumentsMappingIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsMappingTransformer

/**
 * Transform designation into ANNOTATIONS_ARGUMENTS_MAPPING declaration. Affects only for target declaration, it's children and dependents
 */
internal class LLFirDesignatedAnnotationArgumentsMappingTransformer(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirLazyTransformer, FirAnnotationArgumentsMappingTransformer(session, scopeSession, FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) {
    private val ideDeclarationTransformer = LLFirDeclarationTransformer(designation)

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration =
        ideDeclarationTransformer.transformDeclarationContent(this, declaration, data) {
            super.transformDeclarationContent(declaration, data)
        }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) return
        designation.declaration.checkPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)

        ResolveTreeBuilder.resolvePhase(designation.declaration, FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) {
            phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) {
                designation.firFile.transform<FirFile, ResolutionMode>(this, ResolutionMode.ContextIndependent)
            }
        }

        updatePhaseDeep(designation.declaration, FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)
        checkIsResolved(designation.declaration)
    }

    override fun checkIsResolved(declaration: FirDeclaration) {
        declaration.checkPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)
        for (annotation in declaration.annotations) {
            if (annotation is FirAnnotationCall) {
                checkAnnotationArgumentsMappingIsResolved(annotation, declaration)
            }
        }
        checkNestedDeclarationsAreResolved(declaration)
    }
}

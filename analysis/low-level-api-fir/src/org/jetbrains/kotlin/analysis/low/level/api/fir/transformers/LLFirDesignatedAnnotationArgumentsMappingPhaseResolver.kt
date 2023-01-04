/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkAnnotationArgumentsMappingIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsMappingTransformer

/**
 * Transform designation into ANNOTATIONS_ARGUMENTS_MAPPING declaration. Affects only for target declaration, it's children and dependents
 */
internal object LLFirDesignatedAnnotationArgumentsMappingPhaseResolver : LLFirLazyPhaseResolver() {

    override fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        val resolver = LLFirDesignatedAnnotationArgumentsMappingResolver(designation, lockProvider, session, scopeSession)
        resolver.resolve()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(
            target,
            FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING,
            updateForLocalDeclarations = false
        )
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

private class LLFirDesignatedAnnotationArgumentsMappingResolver(
    designation: LLFirDesignationToResolve,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirAbstractBodyResolver(
    designation,
    lockProvider,
    FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING,
) {
    override val transformer =
        FirAnnotationArgumentsMappingTransformer(session, scopeSession, FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

    override fun resolveDeclarationContent(target: FirElementWithResolveState) {
        FirLazyBodiesCalculator.calculateLazyBodiesInside(target.collectDesignationWithFile())
        when (target) {
            is FirRegularClass -> {
                target.transformAnnotations(transformer.declarationsTransformer, ResolutionMode.ContextIndependent)
                target.transformTypeParameters(transformer.declarationsTransformer, ResolutionMode.ContextIndependent)
                target.transformSuperTypeRefs(transformer.declarationsTransformer, ResolutionMode.ContextIndependent)
            }
            is FirCallableDeclaration, is FirAnonymousInitializer, is FirDanglingModifierList, is FirFileAnnotationsContainer, is FirTypeAlias -> {
                target.transform<FirDeclaration, _>(transformer, ResolutionMode.ContextIndependent)
            }
            else -> throwUnexpectedFirElementError(target)
        }
    }
}

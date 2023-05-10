/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsResolveTransformer
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.transformSingle

internal object LLFirAnnotationArgumentsLazyResolver : LLFirLazyResolver(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val resolver = LLFirAnnotationArgumentsTargetResolver(target, lockProvider, session, scopeSession)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        if (target !is FirAnnotationContainer) return
        val unresolvedAnnotation = target.annotations.firstOrNull { it.annotationTypeRef !is FirResolvedTypeRef }
        check(unresolvedAnnotation == null) {
            "Unexpected annotationTypeRef annotation, expected resolvedType but actual ${unresolvedAnnotation?.annotationTypeRef}"
        }

        target.checkPhase(resolverPhase)

        for (annotation in target.annotations) {
            for (argument in annotation.argumentMapping.mapping.values) {
                checkTypeRefIsResolved(argument.typeRef, "annotation argument", target) {
                    withFirEntry("firAnnotation", annotation)
                    withFirEntry("firArgument", argument)
                }
            }
        }

        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirAnnotationArgumentsTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirAbstractBodyTargetResolver(
    target,
    lockProvider,
    scopeSession,
    FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS,
) {
    override val transformer = FirAnnotationArgumentsResolveTransformer(
        session,
        scopeSession,
        resolverPhase,
        returnTypeCalculator = createReturnTypeCalculator(towerDataContextCollector = null)
    )

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        FirLazyBodiesCalculator.calculateAnnotations(target)
        transformer.transformAnnotations(target)
    }
}

internal fun FirAbstractBodyResolveTransformerDispatcher.transformAnnotations(target: FirElementWithResolveState) {
    when {
        target is FirRegularClass -> {
            target.transformAnnotations(declarationsTransformer, ResolutionMode.ContextIndependent)
            target.transformTypeParameters(declarationsTransformer, ResolutionMode.ContextIndependent)
            target.transformSuperTypeRefs(declarationsTransformer, ResolutionMode.ContextIndependent)
        }

        target.isRegularDeclarationWithAnnotation -> {
            target.transformSingle(this, ResolutionMode.ContextIndependent)
        }

        else -> throwUnexpectedFirElementError(target)
    }
}

internal val FirElementWithResolveState.isRegularDeclarationWithAnnotation: Boolean
    get() = when (this) {
        is FirCallableDeclaration,
        is FirAnonymousInitializer,
        is FirDanglingModifierList,
        is FirFileAnnotationsContainer,
        is FirTypeAlias,
        is FirScript,
        -> true
        else -> false
    }
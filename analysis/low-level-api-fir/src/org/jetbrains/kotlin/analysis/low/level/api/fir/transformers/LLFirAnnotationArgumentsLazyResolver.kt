/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsResolveTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle

internal object LLFirAnnotationArgumentsLazyResolver : LLFirLazyResolver(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirResolveContextCollector?,
    ) {
        val resolver = LLFirAnnotationArgumentsTargetResolver(target, lockProvider, session, scopeSession, towerDataContextCollector)
        resolver.resolveDesignation()
    }
}

private class LLFirAnnotationArgumentsTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    firResolveContextCollector: FirResolveContextCollector?,
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
        returnTypeCalculator = createReturnTypeCalculator(firResolveContextCollector = firResolveContextCollector),
        firResolveContextCollector = firResolveContextCollector,
    )

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        transformer.declarationsTransformer.withRegularClass(firClass) {
            action()
            firClass
        }
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        FirLazyBodiesCalculator.calculateAnnotations(target)
        transformAnnotations(target)
    }
}

internal fun LLFirAbstractBodyTargetResolver.transformAnnotations(target: FirElementWithResolveState) {
    when {
        target is FirRegularClass -> transformer.declarationsTransformer?.let { declarationsTransformer ->
            if (this is LLFirAnnotationArgumentsTargetResolver) {
                target.transformAnnotations(declarationsTransformer, ResolutionMode.ContextIndependent)
                target.transformTypeParameters(declarationsTransformer, ResolutionMode.ContextIndependent)
                target.transformSuperTypeRefs(declarationsTransformer, ResolutionMode.ContextIndependent)
            } else {
                declarationsTransformer.context.insideClassHeader {
                    target.transformAnnotations(declarationsTransformer, ResolutionMode.ContextIndependent)
                    target.transformTypeParameters(declarationsTransformer, ResolutionMode.ContextIndependent)
                    target.transformSuperTypeRefs(declarationsTransformer, ResolutionMode.ContextIndependent)
                }
            }
        }

        target is FirScript -> {
            transformer.declarationsTransformer?.let {
                target.transformAnnotations(it, ResolutionMode.ContextIndependent)
            }
        }

        target.isRegularDeclarationWithAnnotation -> {
            target.transformSingle(transformer, ResolutionMode.ContextIndependent)
        }

        target is FirCodeFragment || target is FirFile -> {}

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
        -> true
        else -> false
    }

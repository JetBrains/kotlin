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
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
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

    private inline fun actionWithContextCollector(
        noinline action: () -> Unit,
        crossinline collect: (FirResolveContextCollector, BodyResolveContext) -> Unit,
    ): () -> Unit {
        val collector = transformer.firResolveContextCollector ?: return action
        return {
            collect(collector, transformer.context)
            action()
        }
    }

    override fun withScript(firScript: FirScript, action: () -> Unit) {
        val actionWithCollector = actionWithContextCollector(action) { collector, context ->
            collector.addDeclarationContext(firScript, context)
        }

        super.withScript(firScript, actionWithCollector)
    }

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        val actionWithCollector = actionWithContextCollector(action) { collector, context ->
            collector.addFileContext(firFile, context.towerDataContext)
        }

        super.withFile(firFile, actionWithCollector)
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        val actionWithCollector = actionWithContextCollector(action) { collector, context ->
            collector.addDeclarationContext(firClass, context)
        }

        @Suppress("DEPRECATION_ERROR")
        super.withRegularClassImpl(firClass, actionWithCollector)
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        collectTowerDataContext(target)

        FirLazyBodiesCalculator.calculateAnnotations(target)
        transformAnnotations(target)
    }

    private fun collectTowerDataContext(target: FirElementWithResolveState) {
        val contextCollector = transformer.firResolveContextCollector
        if (contextCollector == null || target !is FirDeclaration) return

        val bodyResolveContext = transformer.context
        withTypeParametersIfMemberDeclaration(bodyResolveContext, target) {
            when (target) {
                is FirRegularClass -> {
                    contextCollector.addClassHeaderContext(target, bodyResolveContext.towerDataContext)
                }

                is FirFunction -> bodyResolveContext.forFunctionBody(target, transformer.components) {
                    contextCollector.addDeclarationContext(target, bodyResolveContext)
                }

                is FirScript -> {}

                else -> contextCollector.addDeclarationContext(target, bodyResolveContext)
            }
        }

        /**
         * [withRegularClass] and [withScript] already have [FirResolveContextCollector.addDeclarationContext] call,
         * so we shouldn't do anything inside
         */
        when (target) {
            is FirRegularClass -> withRegularClass(target) { }
            is FirScript -> withScript(target) { }
            else -> {}
        }
    }

    private inline fun withTypeParametersIfMemberDeclaration(
        context: BodyResolveContext,
        target: FirElementWithResolveState,
        action: () -> Unit,
    ) {
        if (target is FirMemberDeclaration) {
            @OptIn(PrivateForInline::class)
            context.withTypeParametersOf(target, action)
        } else {
            action()
        }
    }
}

internal fun LLFirAbstractBodyTargetResolver.transformAnnotations(target: FirElementWithResolveState) {
    when {
        target is FirRegularClass -> transformer.declarationsTransformer?.let { declarationsTransformer ->
            target.transformAnnotations(declarationsTransformer, ResolutionMode.ContextIndependent)
            target.transformTypeParameters(declarationsTransformer, ResolutionMode.ContextIndependent)
            target.transformSuperTypeRefs(declarationsTransformer, ResolutionMode.ContextIndependent)
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

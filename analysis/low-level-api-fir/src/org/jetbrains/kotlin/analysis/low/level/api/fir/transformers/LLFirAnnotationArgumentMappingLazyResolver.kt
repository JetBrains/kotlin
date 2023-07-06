/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator.calculateAnnotations
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkAnnotationArgumentsMappingIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.expressionGuard
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsMappingTransformer
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

internal object LLFirAnnotationArgumentMappingLazyResolver : LLFirLazyResolver(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirResolveContextCollector?,
    ) {
        val resolver = LLFirAnnotationArgumentsMappingTargetResolver(target, lockProvider, session, scopeSession, towerDataContextCollector)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(resolverPhase)
        if (target !is FirAnnotationContainer) return
        for (annotation in target.annotations) {
            if (annotation is FirAnnotationCall) {
                checkAnnotationArgumentsMappingIsResolved(annotation, target)
            }
        }
        checkNestedDeclarationsAreResolved(target)
    }
}

private class LLFirAnnotationArgumentsMappingTargetResolver(
    resolveTarget: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    firResolveContextCollector: FirResolveContextCollector?,
) : LLFirAbstractBodyTargetResolver(
    resolveTarget,
    lockProvider,
    scopeSession,
    FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING,
) {
    override val transformer = FirAnnotationArgumentsMappingTransformer(
        session,
        scopeSession,
        resolverPhase,
        returnTypeCalculator = createReturnTypeCalculator(firResolveContextCollector = firResolveContextCollector),
        firResolveContextCollector = firResolveContextCollector,
    )

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        resolveWithKeeper(target, AnnotationArgumentMappingStateKeepers.DECLARATION, ::calculateAnnotations) {
            transformAnnotations(target)
        }
    }
}

internal object AnnotationArgumentMappingStateKeepers {
    private val ANNOTATION: StateKeeper<FirAnnotation> = stateKeeper {
        add(ANNOTATION_BASE)
        add(FirAnnotation::argumentMapping, FirAnnotation::replaceArgumentMapping)
        add(FirAnnotation::typeArgumentsCopied, FirAnnotation::replaceTypeArguments)
    }

    private val ANNOTATION_BASE: StateKeeper<FirAnnotation> = stateKeeper { annotation ->
        if (annotation is FirAnnotationCall) {
            entity(annotation, ANNOTATION_CALL)
        }
    }

    private val ANNOTATION_CALL: StateKeeper<FirAnnotationCall> = stateKeeper { annotationCall ->
        add(FirAnnotationCall::calleeReference, FirAnnotationCall::replaceCalleeReference)

        val argumentList = annotationCall.argumentList
        if (argumentList !is FirResolvedArgumentList && argumentList !is FirEmptyArgumentList) {
            add(FirAnnotationCall::argumentList, FirAnnotationCall::replaceArgumentList) { oldList ->
                buildArgumentList {
                    source = oldList.source
                    for (argument in oldList.arguments) {
                        val replacement = when {
                            argument is FirPropertyAccessExpression && argument.calleeReference.isError() -> argument
                            else -> expressionGuard(argument)
                        }
                        arguments.add(replacement)
                    }
                }
            }
        }
    }

    val DECLARATION: StateKeeper<FirElementWithResolveState> = stateKeeper { target ->
        val visitor = object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                when (element) {
                    is FirDeclaration -> if (element !== target) return // Avoid nested declarations
                    is FirAnnotation -> entity(element, ANNOTATION)
                    is FirStatement -> return
                }

                element.acceptChildren(this)
            }
        }

        target.accept(visitor)
    }
}

private val FirAnnotation.typeArgumentsCopied: List<FirTypeProjection>
    get() = if (typeArguments.isEmpty()) emptyList() else ArrayList(typeArguments)
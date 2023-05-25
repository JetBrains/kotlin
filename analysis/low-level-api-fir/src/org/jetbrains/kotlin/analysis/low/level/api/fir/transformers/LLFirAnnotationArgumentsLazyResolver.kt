/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator.calculateAnnotations
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.expressionGuard
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsResolveTransformer
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
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
        resolveWithKeeper(target, AnnotationArgumentsStateKeepers.DECLARATION, ::calculateAnnotations) {
            transformAnnotations(target)
        }
    }
}

internal fun LLFirAbstractBodyTargetResolver.transformAnnotations(target: FirElementWithResolveState) {
    when {
        target is FirRegularClass -> {
            target.transformAnnotations(transformer.declarationsTransformer, ResolutionMode.ContextIndependent)
            target.transformTypeParameters(transformer.declarationsTransformer, ResolutionMode.ContextIndependent)
            target.transformSuperTypeRefs(transformer.declarationsTransformer, ResolutionMode.ContextIndependent)
        }

        target.isRegularDeclarationWithAnnotation -> {
            target.transformSingle(transformer, ResolutionMode.ContextIndependent)
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

@Suppress("PropertyName", "PrivatePropertyName")
internal abstract class AbstractAnnotationStateKeepers {
    protected abstract val ANNOTATION: StateKeeper<FirAnnotation>

    protected val ANNOTATION_BASE: StateKeeper<FirAnnotation> = stateKeeper { annotation ->
        add(FirAnnotation::typeRef, FirAnnotation::replaceTypeRef)
        add(FirAnnotation::annotationTypeRef, FirAnnotation::replaceAnnotationTypeRef)

        if (annotation is FirAnnotationCall) {
            entity(annotation, ANNOTATION_CALL)
        }
    }

    private val ANNOTATION_CALL: StateKeeper<FirAnnotationCall> = stateKeeper { annotationCall ->
        add(FirAnnotationCall::calleeReference, FirAnnotationCall::replaceCalleeReference)
        add(FirAnnotationCall::annotationResolvePhase, FirAnnotationCall::replaceAnnotationResolvePhase)

        val argumentList = annotationCall.argumentList
        if (argumentList !is FirResolvedArgumentList && argumentList !is FirEmptyArgumentList) {
            add(FirAnnotationCall::argumentList, FirAnnotationCall::replaceArgumentList) { oldList ->
                buildArgumentList {
                    source = oldList.source
                    for (argument in oldList.arguments) {
                        arguments.add(expressionGuard(argument))
                    }
                }
            }
        }
    }

    protected open val DECLARATION_BASE: StateKeeper<FirElementWithResolveState> = stateKeeper { target ->
        val visitor = object : FirVisitorVoid() {
            override fun visitDeclaration(declaration: FirDeclaration) {
                if (declaration === target) {
                    // Avoid nested declarations
                    super.visitDeclaration(declaration)
                }
            }

            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitAnnotation(annotation: FirAnnotation) {
                entity(annotation, ANNOTATION)
                super.visitAnnotation(annotation)
            }

            override fun visitStatement(statement: FirStatement) {}
            override fun visitExpression(expression: FirExpression) {}
        }

        target.accept(visitor)
    }
}

private object AnnotationArgumentsStateKeepers : AbstractAnnotationStateKeepers() {
    override val ANNOTATION: StateKeeper<FirAnnotation>
        get() = ANNOTATION_BASE

    val DECLARATION: StateKeeper<FirElementWithResolveState>
        get() = DECLARATION_BASE
}
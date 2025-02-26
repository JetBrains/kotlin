/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDeprecationProviderIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.expressionGuard
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.extensions.withGeneratedDeclarationsSymbolProviderDisabled
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompilerRequiredAnnotationsResolveTransformer
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.util.PrivateForInline

internal object LLFirCompilerAnnotationsLazyResolver : LLFirLazyResolver(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
    override fun createTargetResolver(
        target: LLFirResolveTarget,
    ): LLFirTargetResolver = LLFirCompilerRequiredAnnotationsTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        when (target) {
            is FirClassLikeDeclaration -> checkDeprecationProviderIsResolved(target, target.deprecationsProvider)
            is FirCallableDeclaration -> checkDeprecationProviderIsResolved(target, target.deprecationsProvider)
        }
    }
}

/**
 * This resolver is responsible for [COMPILER_REQUIRED_ANNOTATIONS][FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS] phase.
 *
 * This resolver:
 * - Transforms compiler required annotations of declarations.
 * - Calculates [DeprecationsProvider].
 *
 * @see FirCompilerRequiredAnnotationsResolveTransformer
 * @see FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS
 */
private class LLFirCompilerRequiredAnnotationsTargetResolver(
    target: LLFirResolveTarget,
    computationSession: LLFirCompilerRequiredAnnotationsComputationSession? = null,
) : LLFirTargetResolver(target, FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
    inner class LLFirCompilerRequiredAnnotationsComputationSession : CompilerRequiredAnnotationsComputationSession() {
        override fun resolveAnnotationSymbol(symbol: FirRegularClassSymbol, scopeSession: ScopeSession) {
            val regularClass = symbol.fir
            if (regularClass.resolvePhase >= resolverPhase) return

            symbol.lazyResolveToPhase(resolverPhase.previous)
            val designation = regularClass.collectDesignation().asResolveTarget()
            val resolver = LLFirCompilerRequiredAnnotationsTargetResolver(
                designation,
                this,
            )

            resolver.resolveDesignation()
        }

        override val useCacheForImportScope: Boolean get() = true

        /**
         * In the Analysis API we still need to transform non-source declarations like `componentN` functions
         */
        override val treatNonSourceDeclarationsAsResolved: Boolean get() = false

        /**
         * Annotation arguments should be calculated even if they are not from
         * [org.jetbrains.kotlin.fir.declarations.FirAnnotationsPlatformSpecificSupportComponent.requiredAnnotationsWithArguments]
         * as compiler plugins still may access unresolved arguments for some computations (like to get a class literal)
         */
        override fun annotationResolved(annotation: FirAnnotationCall) {
            FirLazyBodiesCalculator.calculateAnnotation(annotation, resolveTargetSession)
        }
    }

    private val transformer = FirCompilerRequiredAnnotationsResolveTransformer(
        resolveTargetSession,
        resolveTargetScopeSession,
        computationSession ?: LLFirCompilerRequiredAnnotationsComputationSession(),
    )

    @OptIn(PrivateForInline::class)
    private val llFirComputationSession: LLFirCompilerRequiredAnnotationsComputationSession
        get() = transformer.annotationTransformer.computationSession as LLFirCompilerRequiredAnnotationsComputationSession

    /**
     * It is a valid scenario as meta-annotations might have a cycle.
     *
     * Simple example: [Target].
     * The annotation marks itself.
     *
     * Usually such situations can be detected by [CompilerRequiredAnnotationsComputationSession.annotationResolutionWasAlreadyStarted],
     * but in multithreaded scenarios it is not always possible.
     * ```kotlin
     * @Two
     * annotation class One
     *
     * @Three
     * annotation class Two
     *
     * @One
     * annotation class Three
     * ```
     * in this case if two or three classes start resolution at the same time, there will be no any thread that is able
     * to visit all classes with the same [CompilerRequiredAnnotationsComputationSession].
     */
    override fun handleCycleInResolution(target: FirElementWithResolveState) {}

    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    override fun withContainingFile(firFile: FirFile, action: () -> Unit) {
        transformer.annotationTransformer.withFileAndFileScopes(firFile, action)
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        transformer.annotationTransformer.withRegularClass(firClass, action)
    }

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        val alreadyResolved = target is FirAnnotationContainer && llFirComputationSession.annotationsAreResolved(target) ||
                target is FirClassLikeDeclaration && llFirComputationSession.annotationResolutionWasAlreadyStarted(target)

        return alreadyResolved
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        when (target) {
            is FirProperty -> resolve(target, CompilerAnnotationsStateKeepers.PROPERTY)
            is FirFunction -> resolve(target, CompilerAnnotationsStateKeepers.FUNCTION)
            is FirCallableDeclaration -> resolve(target, CompilerAnnotationsStateKeepers.CALLABLE_DECLARATION)
            is FirClassLikeDeclaration -> resolve(target, CompilerAnnotationsStateKeepers.CLASS_LIKE_DECLARATION)
            is FirCodeFragment -> {}
            is FirFile, is FirScript, is FirAnonymousInitializer, is FirDanglingModifierList -> {
                resolve(target, CompilerAnnotationsStateKeepers.ANNOTATION_CONTAINER)
            }

            else -> throwUnexpectedFirElementError(target)
        }
    }

    private fun <T : FirDeclaration> resolve(target: T, keeper: StateKeeper<T, Unit>) {
        resolveWithKeeper(target, Unit, keeper) {
            // N.B. We disable generated declarations provider to avoid infinite resolve problems (see KT-67483)
            @OptIn(FirSymbolProviderInternals::class)
            transformer.session.withGeneratedDeclarationsSymbolProviderDisabled {
                rawResolve(target)
            }
        }
    }

    private fun rawResolve(target: FirDeclaration) {
        val annotationTransformer = transformer.annotationTransformer
        when (target) {
            is FirFile -> annotationTransformer.resolveFile(target) {}
            is FirRegularClass -> annotationTransformer.resolveRegularClass(target) {}
            is FirScript -> annotationTransformer.resolveScript(target) {}
            else -> target.transformSingle(annotationTransformer, null)
        }
    }
}

private object CompilerAnnotationsStateKeepers {
    val PROPERTY: StateKeeper<FirProperty, Unit> = stateKeeper { builder, property, context ->
        builder.entity(property, CALLABLE_DECLARATION, context)
        builder.entity(property.getter, CALLABLE_DECLARATION, context)
        builder.entity(property.setter, CALLABLE_DECLARATION, context)
        builder.entity(property.backingField, CALLABLE_DECLARATION, context)
    }

    val FUNCTION: StateKeeper<FirFunction, Unit> = stateKeeper { builder, function, context ->
        builder.entity(function, CALLABLE_DECLARATION, context)
        builder.entityList(function.valueParameters, CALLABLE_DECLARATION, context)
    }

    val CALLABLE_DECLARATION: StateKeeper<FirCallableDeclaration, Unit> = stateKeeper { builder, declaration, context ->
        builder.add(FirCallableDeclaration::deprecationsProvider, FirCallableDeclaration::replaceDeprecationsProvider)

        builder.entity(declaration, ANNOTATION_CONTAINER, context)
        builder.entityList(declaration.contextParameters, CALLABLE_DECLARATION, context)
    }

    val CLASS_LIKE_DECLARATION: StateKeeper<FirClassLikeDeclaration, Unit> = stateKeeper { builder, declaration, context ->
        builder.add(FirClassLikeDeclaration::deprecationsProvider, FirClassLikeDeclaration::replaceDeprecationsProvider)

        builder.entity(declaration, ANNOTATION_CONTAINER, context)
    }

    val ANNOTATION_CONTAINER: StateKeeper<FirAnnotationContainer, Unit> = stateKeeper { builder, container, context ->
        // For containers where the annotations might be rotated
        builder.add(FirAnnotationContainer::annotations, FirAnnotationContainer::replaceAnnotations)

        builder.entityList(container.annotations, ANNOTATION, context)
    }

    private val ANNOTATION: StateKeeper<FirAnnotation, Unit> = stateKeeper { builder, annotation, context ->
        if (annotation is FirAnnotationCall) {
            builder.entity(annotation, ANNOTATION_CALL, context)
        }
    }

    private val ANNOTATION_CALL: StateKeeper<FirAnnotationCall, Unit> = stateKeeper { builder, annotationCall, _ ->
        builder.add(FirAnnotationCall::annotationResolvePhase, FirAnnotationCall::replaceAnnotationResolvePhase)
        builder.add(FirAnnotationCall::annotationTypeRef, FirAnnotationCall::replaceAnnotationTypeRef)

        if (annotationCall.argumentList !is FirEmptyArgumentList) {
            builder.add(FirAnnotationCall::argumentList, FirAnnotationCall::replaceArgumentList) { oldList ->
                if (oldList.arguments.all { it is FirLazyExpression }) {
                    oldList
                } else {
                    buildArgumentList {
                        source = oldList.source
                        for (argument in oldList.arguments) {
                            arguments.add(expressionGuard(argument))
                        }
                    }
                }
            }
        }
    }
}

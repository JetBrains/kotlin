/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkDeprecationProviderIsResolved
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCallCopy
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompilerRequiredAnnotationsResolveTransformer
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDependentDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isScriptDependentDeclaration

internal object LLFirCompilerAnnotationsLazyResolver : LLFirLazyResolver(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirResolveContextCollector?,
    ) {
        val resolver = LLFirCompilerRequiredAnnotationsTargetResolver(target, lockProvider, session, scopeSession)
        resolver.resolveDesignation()
    }

    override fun phaseSpecificCheckIsResolved(target: FirElementWithResolveState) {
        when (target) {
            is FirClassLikeDeclaration -> checkDeprecationProviderIsResolved(target, target.deprecationsProvider)
            is FirCallableDeclaration -> checkDeprecationProviderIsResolved(target, target.deprecationsProvider)
        }
    }
}

private class LLFirCompilerRequiredAnnotationsTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
    scopeSession: ScopeSession,
    computationSession: LLFirCompilerRequiredAnnotationsComputationSession? = null,
) : LLFirTargetResolver(target, lockProvider, FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS, isJumpingPhase = false) {
    inner class LLFirCompilerRequiredAnnotationsComputationSession : CompilerRequiredAnnotationsComputationSession() {
        override fun resolveAnnotationSymbol(symbol: FirRegularClassSymbol, scopeSession: ScopeSession) {
            val regularClass = symbol.fir
            if (regularClass.resolvePhase >= resolverPhase) return

            symbol.lazyResolveToPhase(resolverPhase.previous)
            val designation = regularClass.collectDesignationWithFile().asResolveTarget()
            val resolver = LLFirCompilerRequiredAnnotationsTargetResolver(
                designation,
                lockProvider,
                designation.target.llFirSession,
                scopeSession,
                this,
            )

            resolver.resolveDesignation()
        }

        override val useCacheForImportScope: Boolean get() = true
    }

    private val transformer = FirCompilerRequiredAnnotationsResolveTransformer(
        session,
        scopeSession,
        computationSession ?: LLFirCompilerRequiredAnnotationsComputationSession(),
    )

    @OptIn(PrivateForInline::class)
    private val llFirComputationSession: LLFirCompilerRequiredAnnotationsComputationSession
        get() = transformer.annotationTransformer.computationSession as LLFirCompilerRequiredAnnotationsComputationSession

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        transformer.annotationTransformer.withFileAndFileScopes(firFile, action)
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        transformer.annotationTransformer.withRegularClass(firClass, action)
    }

    override fun doResolveWithoutLock(target: FirElementWithResolveState): Boolean {
        if (target is FirFile) {
            resolveFileAnnotationContainerIfNeeded(target)
            return false
        }

        when (target) {
            is FirRegularClass, is FirScript, is FirCodeFragment -> {}
            else -> {
                if (!target.isRegularDeclarationWithAnnotation) {
                    throwUnexpectedFirElementError(target)
                }
            }
        }

        requireIsInstance<FirAnnotationContainer>(target)
        resolveTargetDeclaration(target)

        return true
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        if (target is FirFile) return
        throwUnexpectedFirElementError(target)
    }

    private fun <T> resolveTargetDeclaration(target: T) where T : FirAnnotationContainer, T : FirElementWithResolveState {
        // 1. Check that we should process this target
        if (llFirComputationSession.annotationsAreResolved(target, treatNonSourceDeclarationsAsResolved = false)) return

        // 2. Mark this target as resolved to avoid cycle
        llFirComputationSession.recordThatAnnotationsAreResolved(target)

        // 3. Create annotation transformer for targets we want to transform (under read lock)
        // Exit if another thread is already resolved this target
        val annotationTransformer = target.createAnnotationTransformer() ?: return

        // 4. Exit if there are no applicable annotations, so we can just update the phase
        if (annotationTransformer.isNothingToResolve()) {
            return performCustomResolveUnderLock(target) {
                // just update deprecations
                annotationTransformer.publishResult(target)
            }
        }

        // 5. Transform annotations in the air
        annotationTransformer.transformAnnotations()

        // 6. Move some annotations to the proper positions
        annotationTransformer.balanceAnnotations(target)

        // 7. Calculate deprecations in the air
        annotationTransformer.calculateDeprecations(target)

        // 8. Publish result
        performCustomResolveUnderLock(target) {
            annotationTransformer.publishResult(target)
        }
    }

    private fun <T> T.createAnnotationTransformer(): AnnotationTransformer? where T : FirAnnotationContainer, T : FirElementWithResolveState {
        if (!hasAnnotationsToResolve()) {
            return (AnnotationTransformer(mutableMapOf()))
        }

        val map = hashMapOf<FirElementWithResolveState, List<FirAnnotation>>()
        var isUnresolved = false
        withReadLock(this) {
            isUnresolved = true
            annotationsForTransformationTo(map)
        }

        return map.takeIf { isUnresolved }?.let(::AnnotationTransformer)
    }

    private inner class AnnotationTransformer(
        private val annotationMap: MutableMap<FirElementWithResolveState, List<FirAnnotation>>,
    ) {
        private val deprecations: MutableMap<FirElementWithResolveState, DeprecationsProvider> = hashMapOf()

        fun isNothingToResolve(): Boolean = annotationMap.isEmpty()

        fun transformAnnotations() {
            for (annotations in annotationMap.values) {
                for (annotation in annotations) {
                    if (annotation !is FirAnnotationCall) continue
                    val typeRef = annotation.annotationTypeRef as? FirUserTypeRef ?: continue
                    if (!transformer.annotationTransformer.shouldRunAnnotationResolve(typeRef)) continue
                    transformer.annotationTransformer.transformAnnotationCall(annotation, typeRef)
                }
            }
        }

        fun balanceAnnotations(target: FirElementWithResolveState) {
            if (target !is FirProperty) return
            val backingField = target.backingField ?: return
            val updatedAnnotations = transformer.annotationTransformer.extractBackingFieldAnnotationsFromProperty(
                target,
                annotationMap[target].orEmpty(),
                annotationMap[backingField].orEmpty(),
            ) ?: return

            annotationMap[target] = updatedAnnotations.propertyAnnotations
            annotationMap[backingField] = updatedAnnotations.backingFieldAnnotations
        }

        fun calculateDeprecations(target: FirElementWithResolveState) {
            val session = target.llFirSession
            val cacheFactory = session.firCachesFactory
            if (target is FirProperty) {
                deprecations[target] = target.extractDeprecationInfoPerUseSite(
                    session = session,
                    customAnnotations = annotationMap[target].orEmpty(),
                    getterAnnotations = target.getter?.let(annotationMap::get).orEmpty(),
                    setterAnnotations = target.setter?.let(annotationMap::get).orEmpty(),
                ).toDeprecationsProvider(cacheFactory)
            }

            for ((declaration, annotations) in annotationMap) {
                if (declaration is FirProperty || declaration is FirFileAnnotationsContainer) continue

                requireIsInstance<FirAnnotationContainer>(declaration)
                deprecations[declaration] = declaration.extractDeprecationInfoPerUseSite(
                    session = session,
                    customAnnotations = annotations,
                ).toDeprecationsProvider(cacheFactory)
            }
        }

        fun <T> publishResult(target: T) where T : FirElementWithResolveState, T : FirAnnotationContainer {
            val newAnnotations = annotationMap[target]
            if (newAnnotations != null) {
                target.replaceAnnotations(newAnnotations)
            }

            val deprecationProvider = deprecations[target] ?: EmptyDeprecationsProvider
            when (target) {
                is FirClassLikeDeclaration -> target.replaceDeprecationsProvider(deprecationProvider)
                is FirCallableDeclaration -> target.replaceDeprecationsProvider(deprecationProvider)
            }

            when (target) {
                is FirFunction -> target.valueParameters.forEach(::publishResult)
                is FirProperty -> {
                    target.getter?.let(::publishResult)
                    target.setter?.let(::publishResult)
                    target.backingField?.let(::publishResult)
                }

                is FirScript -> target.forEachDependentDeclaration(::publishResult)
            }
        }
    }

    /**
     * @return true if at least one applicable annotation is present
     */
    private fun <T> T.annotationsForTransformationTo(
        map: MutableMap<FirElementWithResolveState, List<FirAnnotation>>,
    ) where T : FirAnnotationContainer, T : FirElementWithResolveState {
        when (this) {
            is FirFunction -> {
                valueParameters.forEach {
                    it.annotationsForTransformationTo(map)
                }
            }

            is FirProperty -> {
                getter?.annotationsForTransformationTo(map)
                setter?.annotationsForTransformationTo(map)
                backingField?.annotationsForTransformationTo(map)
            }

            is FirScript -> {
                forEachDependentDeclaration { it.annotationsForTransformationTo(map) }
            }
        }

        if (annotations.isEmpty()) return

        var hasApplicableAnnotation = false
        val containerForAnnotations = ArrayList<FirAnnotation>(annotations.size)
        for (annotation in annotations) {
            val userTypeRef = (annotation as? FirAnnotationCall)?.annotationTypeRef as? FirUserTypeRef
            containerForAnnotations += if (userTypeRef != null && transformer.annotationTransformer.shouldRunAnnotationResolve(userTypeRef)) {
                hasApplicableAnnotation = true
                buildAnnotationCallCopy(annotation) {
                    /**
                     * CRA transformer won't modify this type reference,
                     * but we create a deep copy
                     * to be sure that no one another thread can't modify this reference during another phase,
                     * while we do deep copy during
                     * [org.jetbrains.kotlin.fir.resolve.transformers.plugin.AbstractFirSpecificAnnotationResolveTransformer.transformAnnotationCall]
                     * without lock
                     */
                    annotationTypeRef = transformer.annotationTransformer.createDeepCopyOfTypeRef(userTypeRef)

                    // We assume that non-empty argument must be a lazy expression, or it is a copied declaration inside an on-air session
                    if (FirLazyBodiesCalculator.needCalculatingAnnotationCall(annotation)) {
                        argumentList = FirLazyBodiesCalculator.calculateLazyArgumentsForAnnotation(annotation, llFirSession)
                    }
                }
            } else {
                annotation
            }
        }

        if (hasApplicableAnnotation) {
            map[this] = containerForAnnotations
        }
    }
}

private fun FirAnnotationContainer.hasAnnotationsToResolve(): Boolean {
    if (annotations.isNotEmpty()) return true

    return when (this) {
        is FirFunction -> valueParameters.any(FirAnnotationContainer::hasAnnotationsToResolve)
        is FirProperty -> this.getter?.hasAnnotationsToResolve() == true ||
                this.setter?.hasAnnotationsToResolve() == true ||
                this.backingField?.hasAnnotationsToResolve() == true

        is FirScript -> statements.any { it.isScriptDependentDeclaration && it.hasAnnotationsToResolve() }
        else -> false
    }
}

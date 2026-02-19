/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.structure

import org.jetbrains.kotlin.analysis.api.platform.statistics.KotlinObjectSizeCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibrarySession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsOnlyApi
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLFirJavaSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLKotlinStubBasedLibrarySymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.nullableJavaSymbolProvider
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.FirLazyJavaDeclarationList
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaAnnotationList
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaDeclarationList
import org.jetbrains.kotlin.fir.java.enhancement.FirLazyJavaAnnotationList
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import kotlin.time.DurationUnit

@OptIn(LLStatisticsOnlyApi::class)
internal object LLSessionStatisticsCalculator {
    /**
     * Calculates the weight and other statistics for the given [session].
     */
    fun calculateSessionStatistics(session: LLFirSession): LLSessionStatistics {
        val objectSizeCalculator = KotlinObjectSizeCalculator.getInstance(session.project)

        return context(objectSizeCalculator) {
            when (session) {
                is LLFirResolvableModuleSession -> calculateResolvableSessionStatistics(session)
                is LLFirLibrarySession -> calculateLibrarySessionStatistics(session)
                else -> LLSessionStatistics.ZERO
            }
        }
    }

    context(_: KotlinObjectSizeCalculator?)
    private fun calculateResolvableSessionStatistics(session: LLFirResolvableModuleSession): LLSessionStatistics {
        val moduleFileCache = session.moduleComponents.cache
        val firFiles = moduleFileCache.getAllCachedFirFiles()

        val kotlinWeight = calculateFirElementWeight(firFiles)

        val javaWeight = (session.nullableJavaSymbolProvider as? LLFirJavaSymbolProvider)
            ?.cachedDeclarations
            ?.let { calculateFirElementWeight(it) }
            ?: 0L

        return LLSessionStatistics(kotlinWeight, javaWeight, session.currentLifetime)
    }

    context(_: KotlinObjectSizeCalculator?)
    private fun calculateLibrarySessionStatistics(session: LLFirLibrarySession): LLSessionStatistics {
        val symbolProviders = (session.symbolProvider as? LLModuleWithDependenciesSymbolProvider)?.providers
            ?: return LLSessionStatistics.ZERO

        val kotlinWeight = symbolProviders
            .filterIsInstance<LLKotlinStubBasedLibrarySymbolProvider>()
            .sumOf { calculateFirElementWeight(it.cachedDeclarations) }

        val javaWeight = symbolProviders
            .filterIsInstance<LLFirJavaSymbolProvider>()
            .sumOf { calculateFirElementWeight(it.cachedDeclarations) }

        return LLSessionStatistics(kotlinWeight, javaWeight, session.currentLifetime)
    }

    private val LLFirSession.currentLifetime: Double
        get() = creationTimeMark.elapsedNow().toDouble(DurationUnit.SECONDS)

    context(objectSizeCalculator: KotlinObjectSizeCalculator?)
    private fun calculateFirElementWeight(firElements: Collection<FirElement>): Long {
        if (objectSizeCalculator == null) return 0L

        return firElements.sumOf { calculateFirElementWeight(it) }
    }

    context(objectSizeCalculator: KotlinObjectSizeCalculator)
    private fun calculateFirElementWeight(firElement: FirElement): Long {
        val visitor = FirElementWeightCalculatorVisitor(objectSizeCalculator)
        firElement.accept(visitor, null)
        return visitor.totalWeight
    }

    /**
     * FIR element weight calculation is an approximation. See [LLSessionStatistics] for details.
     */
    private class FirElementWeightCalculatorVisitor(private val objectSizeCalculator: KotlinObjectSizeCalculator) : FirVisitorVoid() {
        var totalWeight = 0L
            private set

        override fun visitElement(element: FirElement) {
            totalWeight += objectSizeCalculator.shallowSize(element)

            // FIR Java elements contain lazily computed properties. When logging the session structure, it is not feasible to perform all
            // of these computations at once because it would take too much time. Any corresponding read action would likely be canceled.
            // And furthermore, we want to log everything *currently* cached, so we should avoid computing lazy properties.
            //
            // Unfortunately, it is not easy to turn off this laziness, and the API is transparent. Therefore, we have to use reflection to
            // check the status of known lazy properties. If lazy properties are added or removed in the future, the visitor will likely
            // break. As this is simply a diagnostic (non-production) feature, that's perfectly fine. Missing child traversals will not lead
            // to an error (i.e., when a FIR Java element adds a new line to its `acceptChildren`), but this will at most make the weight
            // calculation slightly less precise.
            //
            // Lazy FIR Java elements are mainly a workaround for KT-55387, so with a better architecture, the lazy avoidance wouldn't be
            // needed.
            when (element) {
                is FirJavaClass -> {
                    visitJavaDeclarationList(element.declarationList)
                    visitJavaAnnotationList(element.annotations)
                    if (FirJavaClass::typeParameters.isLazyInitialized(element)) {
                        element.typeParameters.forEach { it.accept(this, null) }
                    }
                    if (FirJavaClass::status.isLazyInitialized(element)) {
                        element.status.accept(this, null)
                    }
                    if (FirJavaClass::superTypeRefs.isLazyInitialized(element)) {
                        element.superTypeRefs.forEach { it.accept(this, null) }
                    }
                }

                is FirJavaConstructor -> {
                    element.returnTypeRef.accept(this, null)
                    element.controlFlowGraphReference?.accept(this, null)
                    element.typeParameters.forEach { it.accept(this, null) }
                    element.valueParameters.forEach { it.accept(this, null) }
                    if (FirJavaConstructor::status.isLazyInitialized(element)) {
                        element.status.accept(this, null)
                    }
                    visitJavaAnnotationList(element.annotations)
                }

                is FirJavaField -> {
                    element.returnTypeRef.accept(this, null)
                    visitJavaAnnotationList(element.annotations)
                    element.typeParameters.forEach { it.accept(this, null) }
                    if (FirJavaField::status.isLazyInitialized(element)) {
                        element.status.accept(this, null)
                    }
                    if (element.lazyInitializer.isInitialized()) {
                        element.initializer?.accept(this, null)
                    }
                }

                is FirJavaMethod -> {
                    element.returnTypeRef.accept(this, null)
                    element.receiverParameter?.accept(this, null)
                    element.controlFlowGraphReference?.accept(this, null)
                    element.valueParameters.forEach { it.accept(this, null) }
                    element.body?.accept(this, null)
                    if (FirJavaMethod::status.isLazyInitialized(element)) {
                        element.status.accept(this, null)
                    }
                    visitJavaAnnotationList(element.annotations)
                    element.typeParameters.forEach { it.accept(this, null) }
                }

                is FirJavaTypeParameter -> {
                    // We skip type parameter bounds because the property is not lazy but performs heavy calculations.

                    visitJavaAnnotationList(element.annotations)
                }

                is FirJavaValueParameter -> {
                    element.returnTypeRef.accept(this, null)
                    visitJavaAnnotationList(element.annotations)
                    if (element.lazyDefaultValue?.isInitialized() == true) {
                        element.defaultValue?.accept(this, null)
                    }
                }

                is FirJavaTypeRef -> {
                    if (FirJavaTypeRef::annotations.isLazyInitialized(element)) {
                        element.annotations.forEach { it.accept(this, null) }
                    }
                }

                else -> element.acceptChildren(this, null)
            }
        }

        @OptIn(FirImplementationDetail::class)
        private fun visitJavaDeclarationList(declarationList: FirJavaDeclarationList) {
            if (declarationList is FirLazyJavaDeclarationList) {
                if (FirLazyJavaDeclarationList::declarations.isLazyInitialized(declarationList)) {
                    declarationList.declarations.forEach { it.accept(this, null) }
                }
            }
        }

        private fun visitJavaAnnotationList(annotationList: List<FirAnnotation>) {
            val annotations = when (annotationList) {
                is FirLazyJavaAnnotationList -> if (annotationList.isInitialized) annotationList else null
                is FirJavaAnnotationList -> null // Defensively ignore `FirDelegatedJavaAnnotationList`.
                else -> annotationList
            }

            annotations?.forEach { it.accept(this, null) }
        }
    }
}

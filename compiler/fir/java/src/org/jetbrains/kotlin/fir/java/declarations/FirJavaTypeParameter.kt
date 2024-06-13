/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.Companion.ANALYZED_DEPENDENCIES
import org.jetbrains.kotlin.fir.declarations.FirResolvedToPhaseState
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.FirJavaTypeConversionMode
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class)
class FirJavaTypeParameter(
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val name: Name,
    override val symbol: FirTypeParameterSymbol,
    override val containingDeclarationSymbol: FirBasedSymbol<*>,
    private var initialBounds: List<FirTypeRef>?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
) : FirTypeParameter() {

    private enum class BoundsEnhancementState {
        NOT_STARTED,
        FIRST_ROUND,
        COMPLETED
    }

    override val variance: Variance
        get() = Variance.INVARIANT

    override val isReified: Boolean
        get() = false

    @Volatile
    private var enhancedBounds: List<FirResolvedTypeRef>? = null

    @Volatile
    private var boundsEnhancementState = BoundsEnhancementState.NOT_STARTED

    override val bounds: List<FirTypeRef>
        get() {
            enhancedBounds?.let { return it }
            if (containingDeclarationSymbol is FirClassSymbol) {
                error(
                    "Attempt to access Java type parameter bounds before their enhancement!" +
                            " ownerSymbol = $containingDeclarationSymbol typeParameter = $name"
                )
            }
            // It's possible to get here for FirJavaMethod via JavaOverrideChecker
            // Stack trace: (JavaOverrideChecker).isOverriddenFunction -> hasSameValueParameterTypes ->
            // buildTypeParametersSubstitutorIfCompatible -> buildErasure
            // For JavaOverrideChecker it's possible to work with not-yet-enhanced bounds
            return initialBounds!!
        }

    init {
        symbol.bind(this)
        resolveState = FirResolvedToPhaseState(ANALYZED_DEPENDENCIES)
    }

    /**
     * This function is assumed to be called under facade- or method type parameter bounds lock.
     * It never tries to resolve some other type parameter bounds, e.g. for a different class.
     * Mutates [enhancedBounds].
     *
     * @return true if the bounds were changed, false if the first round had been already performed earlier
     */
    internal fun performFirstRoundOfBoundsResolution(
        session: FirSession,
        javaTypeParameterStack: JavaTypeParameterStack,
        source: KtSourceElement?,
    ): Boolean {
        if (boundsEnhancementState != BoundsEnhancementState.NOT_STARTED) {
            return false
        }
        boundsEnhancementState = BoundsEnhancementState.FIRST_ROUND
        enhancedBounds = initialBounds!!.mapTo(mutableListOf()) {
            it.resolveIfJavaType(
                session, javaTypeParameterStack, source, FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND
            ) as FirResolvedTypeRef
        }
        return true
    }

    /**
     * This function shouldn't be called under lock. It mutates nothing.
     *
     * @return a mutable list of bound, enhanced to the 2nd round, or null if bounds were already enhanced in the past
     */
    internal fun performSecondRoundOfBoundsResolution(
        session: FirSession,
        javaTypeParameterStack: JavaTypeParameterStack,
        source: KtSourceElement?,
    ): MutableList<FirResolvedTypeRef>? {
        if (boundsEnhancementState != BoundsEnhancementState.FIRST_ROUND) {
            require(boundsEnhancementState == BoundsEnhancementState.COMPLETED) {
                "Attempt to miss the first round of Java class type parameter bounds enhancement!" +
                        " ownerSymbol = $containingDeclarationSymbol typeParameter = $name"
            }
            return null
        }
        return initialBounds!!.mapTo(mutableListOf()) {
            it.resolveIfJavaType(
                session, javaTypeParameterStack, source, FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND
            ) as FirResolvedTypeRef
        }
    }

    /**
     * This function is assumed to be called under facade- or method type parameter bounds lock.
     * Performs a final mutation of [enhancedBounds].
     * Silently does nothing if bounds was already enhanced in the past.
     */
    internal fun storeBoundsAfterAllRounds(bounds: List<FirResolvedTypeRef>) {
        if (boundsEnhancementState != BoundsEnhancementState.FIRST_ROUND) {
            return
        }
        boundsEnhancementState = BoundsEnhancementState.COMPLETED
        enhancedBounds = bounds
        initialBounds = null
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        bounds.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaTypeParameter {
        shouldNotBeCalled()
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaTypeParameter {
        shouldNotBeCalled()
    }

    override fun replaceBounds(newBounds: List<FirTypeRef>) {
        shouldNotBeCalled()
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }
}

@FirBuilderDsl
class FirJavaTypeParameterBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var name: Name
    lateinit var symbol: FirTypeParameterSymbol
    lateinit var containingDeclarationSymbol: FirBasedSymbol<*>
    val bounds: MutableList<FirTypeRef> = mutableListOf()
    override val annotations: MutableList<FirAnnotation> = mutableListOf()

    override fun build(): FirTypeParameter {
        return FirJavaTypeParameter(
            source,
            moduleData,
            origin,
            attributes,
            name,
            symbol,
            containingDeclarationSymbol,
            bounds.takeIf { it.isNotEmpty() } ?: emptyList(),
            annotations.toMutableOrEmpty(),
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildJavaTypeParameter(init: FirJavaTypeParameterBuilder.() -> Unit): FirTypeParameter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirJavaTypeParameterBuilder().apply(init).build()
}

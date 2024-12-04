/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.FirJavaTypeConversionMode
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.enhancement.FirEmptyJavaAnnotationList
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaAnnotationList
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class)
class FirJavaTypeParameter(
    internal val javaTypeParameter: JavaTypeParameter,
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val name: Name,
    override val symbol: FirTypeParameterSymbol,
    override val containingDeclarationSymbol: FirBasedSymbol<*>,
    private var initialBounds: List<FirTypeRef>?,
    private val annotationList: FirJavaAnnotationList,
) : FirTypeParameter() {
    override val annotations: List<FirAnnotation> get() = annotationList

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
                val firJavaClass = containingDeclarationSymbol.fir
                checkWithAttachment(
                    firJavaClass is FirJavaClass,
                    { "Unexpected containing declaration: ${firJavaClass::class.simpleName}" }
                ) {
                    withFirEntry("class", firJavaClass)
                }

                // Explicitly call type parameters which will trigger enhancement
                firJavaClass.typeParameters

                // Second attempt after enhancement
                enhancedBounds?.let { return it }

                errorWithAttachment("Attempt to access Java type parameter bounds before their enhancement!") {
                    withFirEntry("class", firJavaClass)
                    withEntry("name", name.asString())
                }
            }

            // It's possible to get here for FirJavaMethod via JavaOverrideChecker
            // Stack trace: (JavaOverrideChecker).isOverriddenFunction -> hasSameValueParameterTypes ->
            // buildTypeParametersSubstitutorIfCompatible -> buildErasure
            // For JavaOverrideChecker it's possible to work with not-yet-enhanced bounds
            return initialBounds!!
        }

    init {
        symbol.bind(this)
        resolveState = FirResolvePhase.ANALYZED_DEPENDENCIES.asResolveState()
    }

    /**
     * This function shouldn't be called under lock. It mutates nothing.
     *
     * @return a list of bounds, enhanced to the first round, or null if bounds were already enhanced in the past
     */
    internal fun performFirstRoundOfBoundsResolution(
        javaTypeParameterStack: JavaTypeParameterStack,
        source: KtSourceElement?,
    ): MutableList<FirResolvedTypeRef>? {
        if (boundsEnhancementState != BoundsEnhancementState.NOT_STARTED) {
            return null
        }

        return initialBounds!!.mapTo(mutableListOf()) {
            it.resolveIfJavaType(
                moduleData.session, javaTypeParameterStack, source, FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND
            ) as FirResolvedTypeRef
        }
    }

    /**
     * This function is assumed to be called under facade- or method type parameter bounds lock.
     * It never tries to resolve some other type parameter bounds, e.g., for a different class.
     * Mutates [enhancedBounds].
     *
     * @return true if the bounds were changed, false if the first round had been already performed earlier
     */
    internal fun storeBoundsAfterFirstRound(bounds: List<FirResolvedTypeRef>): Boolean {
        if (boundsEnhancementState != BoundsEnhancementState.NOT_STARTED) {
            return false
        }

        boundsEnhancementState = BoundsEnhancementState.FIRST_ROUND
        enhancedBounds = bounds
        return true
    }

    /**
     * This function shouldn't be called under lock. It mutates nothing.
     *
     * @return a mutable list of bound, enhanced to the second round, or null if bounds were already enhanced in the past
     */
    internal fun performSecondRoundOfBoundsResolution(
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
                moduleData.session, javaTypeParameterStack, source, FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND
            ) as FirResolvedTypeRef
        }
    }

    /**
     * This function is assumed to be called under facade- or method type parameter bounds lock.
     * Performs a final mutation of [enhancedBounds].
     *
     * @return **false** if bounds were already enhanced in the past.
     */
    internal fun storeBoundsAfterSecondRound(bounds: List<FirResolvedTypeRef>): Boolean {
        if (boundsEnhancementState != BoundsEnhancementState.FIRST_ROUND) {
            return false
        }

        boundsEnhancementState = BoundsEnhancementState.COMPLETED
        enhancedBounds = bounds
        initialBounds = null
        return true
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        bounds.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaTypeParameter {
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaTypeParameter {
        return this
    }

    override fun replaceBounds(newBounds: List<FirTypeRef>) {
        shouldNotBeCalled(::replaceBounds, ::bounds)
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        shouldNotBeCalled(::replaceAnnotations, ::annotations)
    }
}

@FirBuilderDsl
class FirJavaTypeParameterBuilder {
    var source: KtSourceElement? = null
    lateinit var moduleData: FirModuleData
    lateinit var origin: FirDeclarationOrigin
    var attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    lateinit var name: Name
    lateinit var symbol: FirTypeParameterSymbol
    lateinit var containingDeclarationSymbol: FirBasedSymbol<*>
    val bounds: MutableList<FirTypeRef> = mutableListOf()
    lateinit var annotationBuilder: () -> List<FirAnnotation>
    var annotationList: FirJavaAnnotationList = FirEmptyJavaAnnotationList
    lateinit var javaTypeParameter: JavaTypeParameter

    fun build(): FirTypeParameter {
        return FirJavaTypeParameter(
            javaTypeParameter,
            source,
            moduleData,
            origin,
            attributes,
            name,
            symbol,
            containingDeclarationSymbol,
            bounds.takeIf { it.isNotEmpty() } ?: emptyList(),
            annotationList,
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

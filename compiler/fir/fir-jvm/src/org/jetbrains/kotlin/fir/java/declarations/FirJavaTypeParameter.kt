/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
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
import org.jetbrains.kotlin.fir.java.toFirJavaTypeRef
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(FirImplementationDetail::class, ResolveStateAccess::class)
class FirJavaTypeParameter(
    val javaTypeParameter: JavaTypeParameter,
    override val source: KtSourceElement?,
    override val moduleData: FirModuleData,
    override val origin: FirDeclarationOrigin,
    override val attributes: FirDeclarationAttributes,
    override val name: Name,
    override val symbol: FirTypeParameterSymbol,
    override val containingDeclarationSymbol: FirBasedSymbol<*>,
    private val annotationList: FirJavaAnnotationList,
) : FirTypeParameter() {
    /**
     * [JavaTypeParameter.upperBounds] requires resolution, so it should be called on demand.
     */
    @Volatile
    private var boundsEnhancementState: BoundsEnhancementState = BoundsEnhancementState.NotFinished.NotStarted(
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            val session = moduleData.session
            val fakeSource = source?.fakeElement(KtFakeSourceElementKind.Enhancement)
            javaTypeParameter.upperBounds.map {
                it.toFirJavaTypeRef(session, fakeSource)
            }.ifEmpty {
                val builtinTypes = session.builtinTypes
                listOf(buildResolvedTypeRef {
                    coneType = ConeFlexibleType(builtinTypes.anyType.coneType, builtinTypes.nullableAnyType.coneType, isTrivial = true)
                })
            }
        }
    )

    override val annotations: List<FirAnnotation> get() = annotationList

    /**
     * The class represents the state of the enhancement process for the type parameter.
     */
    private sealed class BoundsEnhancementState {
        /**
         * Either the initial bounds or the final enhanced bounds.
         */
        abstract val bounds: List<FirTypeRef>

        /**
         * Either bounds after the [first round][performFirstRoundOfBoundsResolution] of enhancement or the [final enhanced][performSecondRoundOfBoundsResolution] bounds.
         *
         * @see storeBoundsAfterFirstRound
         * @see storeBoundsAfterSecondRound
         */
        abstract val enhancedBounds: List<FirResolvedTypeRef>?

        sealed class NotFinished(val initialBounds: Lazy<List<FirTypeRef>>) : BoundsEnhancementState() {
            override val bounds: List<FirTypeRef> get() = initialBounds.value

            /**
             * @see performFirstRoundOfBoundsResolution
             * @see storeBoundsAfterFirstRound
             */
            class NotStarted(initialBounds: Lazy<List<FirTypeRef>>) : NotFinished(initialBounds) {
                override val enhancedBounds: List<FirResolvedTypeRef>? get() = null
            }

            /**
             * @see performSecondRoundOfBoundsResolution
             * @see storeBoundsAfterFirstRound
             * @see storeBoundsAfterSecondRound
             */
            class FirstRound(
                initialBounds: Lazy<List<FirTypeRef>>,
                override val enhancedBounds: List<FirResolvedTypeRef>,
            ) : NotFinished(initialBounds)
        }

        /**
         * Contains the final [enhancedBounds].
         *
         * @see storeBoundsAfterSecondRound
         */
        class Completed(override val enhancedBounds: List<FirResolvedTypeRef>) : BoundsEnhancementState() {
            override val bounds: List<FirTypeRef> get() = enhancedBounds
        }
    }

    override val variance: Variance
        get() = Variance.INVARIANT

    override val isReified: Boolean
        get() = false

    override val bounds: List<FirTypeRef>
        get() {
            run {
                val initialState = boundsEnhancementState
                initialState.enhancedBounds?.let { return it }

                if (containingDeclarationSymbol !is FirClassSymbol) {
                    // It's possible to get here for FirJavaMethod via JavaOverrideChecker
                    // Stack trace: (JavaOverrideChecker).isOverriddenFunction -> hasSameValueParameterTypes ->
                    // buildTypeParametersSubstitutorIfCompatible -> buildErasure
                    // For JavaOverrideChecker it's possible to work with not-yet-enhanced bounds
                    return initialState.bounds
                }
            }

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
            val currentState = boundsEnhancementState
            currentState.enhancedBounds?.let { return it }

            errorWithAttachment(
                "Attempt to access Java type parameter bounds before their enhancement! (state: ${currentState::class.simpleName})"
            ) {
                withFirEntry("class", firJavaClass)
                withEntry("name", name.asString())
            }
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
        val currentState = boundsEnhancementState
        if (currentState !is BoundsEnhancementState.NotFinished.NotStarted) {
            return null
        }

        return currentState.bounds.mapTo(mutableListOf()) {
            it.resolveIfJavaType(
                moduleData.session, javaTypeParameterStack, source, FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND
            ) as FirResolvedTypeRef
        }
    }

    /**
     * This function is assumed to be called under facade- or method type parameter bounds lock.
     * It never tries to resolve some other type parameter bounds, e.g., for a different class.
     *
     * @return true if the bounds were changed, false if the first round had been already performed earlier
     */
    internal fun storeBoundsAfterFirstRound(bounds: List<FirResolvedTypeRef>): Boolean {
        val currentState = boundsEnhancementState
        if (currentState !is BoundsEnhancementState.NotFinished.NotStarted) {
            return false
        }

        boundsEnhancementState = BoundsEnhancementState.NotFinished.FirstRound(currentState.initialBounds, bounds)
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
        val currentState = boundsEnhancementState
        if (currentState !is BoundsEnhancementState.NotFinished.FirstRound) {
            requireWithAttachment(currentState is BoundsEnhancementState.Completed, {
                "Attempt to miss the first round of Java class type parameter bounds enhancement!"
            }) {
                withFirEntry("owner", containingDeclarationSymbol.fir)
                withFirEntry("parameter", this@FirJavaTypeParameter)
            }

            return null
        }

        return currentState.bounds.mapTo(mutableListOf()) {
            it.resolveIfJavaType(
                moduleData.session, javaTypeParameterStack, source, FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND
            ) as FirResolvedTypeRef
        }
    }

    /**
     * This function is assumed to be called under facade- or method type parameter bounds lock.
     *
     * @return **false** if bounds were already enhanced in the past.
     */
    internal fun storeBoundsAfterSecondRound(bounds: List<FirResolvedTypeRef>): Boolean {
        val currentState = boundsEnhancementState
        if (currentState !is BoundsEnhancementState.NotFinished.FirstRound) {
            return false
        }

        boundsEnhancementState = BoundsEnhancementState.Completed(bounds)
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

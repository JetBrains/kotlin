/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirComposableSessionComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.explicitTypeArgumentIfMadeFlexibleSynthetically
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import kotlin.reflect.KClass

/**
 * Recursively analyzes type parameters and reports the diagnostic on the given source calculated using typeRef
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkUpperBoundViolated(
    typeRef: FirTypeRef?,
    isIgnoreTypeParameters: Boolean = false,
    isInsideTypeOperatorOrParameterBounds: Boolean = false,
) {
    val type = typeRef?.coneType?.lowerBoundIfFlexible() as? ConeClassLikeType ?: return
    checkUpperBoundViolated(typeRef, type, isIgnoreTypeParameters, typeRef.source, isInsideTypeOperatorOrParameterBounds)
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkUpperBoundViolated(
    typeRef: FirTypeRef?,
    notExpandedType: ConeClassLikeType,
    isIgnoreTypeParameters: Boolean = false,
    fallbackSource: KtSourceElement?,
    isInsideTypeOperatorOrParameterBounds: Boolean = false,
) {
    // If we have FirTypeRef information, add KtSourceElement information to each argument of the type and fully expand.
    val type = if (typeRef != null) {
        (notExpandedType.abbreviatedTypeOrSelf as? ConeClassLikeType)
            ?.fullyExpandedTypeWithSource(typeRef, context.session)
            // Add fallback source information to arguments of the expanded type.
            ?.withArguments { it.withSource(FirTypeRefSource(null, typeRef.source)) }
            ?: return
    } else {
        notExpandedType.fullyExpandedType()
    }

    if (type.typeArguments.isEmpty()) return

    val prototypeClassSymbol = type.lookupTag.toRegularClassSymbol() ?: return

    val typeParameterSymbols = prototypeClassSymbol.typeParameterSymbols

    if (typeParameterSymbols.isEmpty()) {
        return
    }

    val substitution = typeParameterSymbols.zip(type.typeArguments).toMap()
    val substitutor = FE10LikeConeSubstitutor(substitution, context.session)

    return checkUpperBoundViolated(
        typeParameterSymbols,
        type.typeArguments.toList(),
        substitutor,
        isReportExpansionError = true,
        isIgnoreTypeParameters,
        fallbackSource,
        isInsideTypeOperatorOrParameterBounds,
    )
}

fun List<FirTypeProjection>.toTypeArgumentsWithSourceInfo(): List<ConeTypeProjection> {
    return map { firTypeProjection ->
        firTypeProjection.toConeTypeProjection().withSource(
            FirTypeRefSource((firTypeProjection as? FirTypeProjectionWithVariance)?.typeRef, firTypeProjection.source)
        )
    }
}

fun createSubstitutorForUpperBoundViolationCheck(
    typeParameters: List<FirTypeParameterSymbol>,
    typeArguments: List<ConeTypeProjection>,
    session: FirSession
): ConeSubstitutor {
    return substitutorByMap(
        typeParameters.withIndex().associate { Pair(it.value, typeArguments[it.index] as ConeKotlinType) },
        session,
    )
}

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkUpperBoundViolated(
    typeParameters: List<FirTypeParameterSymbol>,
    typeArguments: List<ConeTypeProjection>,
    substitutor: ConeSubstitutor,
    isReportExpansionError: Boolean = false,
    isIgnoreTypeParameters: Boolean = false,
    fallbackSource: KtSourceElement?,
    isInsideTypeOperatorOrParameterBounds: Boolean = false,
) {
    val count = minOf(typeParameters.size, typeArguments.size)
    val typeSystemContext = context.session.typeContext
    val additionalUpperBoundsProviders = context.session.platformUpperBoundsProviders

    for (index in 0 until count) {
        val argument = typeArguments[index]
        val argumentType = argument.type
        val sourceAttribute = argumentType?.attributes?.sourceAttribute
        val argumentTypeRef = sourceAttribute?.typeRef
        val argumentSource = sourceAttribute?.source

        if (argumentType != null) {
            val beStrict = context.languageVersionSettings.supportsFeature(LanguageFeature.DontIgnoreUpperBoundViolatedOnImplicitArguments)
            val regularDiagnostic = when {
                isExplicitTypeArgumentSource(argumentSource) || beStrict -> FirErrors.UPPER_BOUND_VIOLATED
                else -> FirErrors.UPPER_BOUND_VIOLATED_DEPRECATION_WARNING
            }
            val typealiasDiagnostic = when {
                isExplicitTypeArgumentSource(argumentSource) || beStrict -> FirErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION
                else -> FirErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING
            }
            if (!isIgnoreTypeParameters || (argumentType.typeArguments.isEmpty() && argumentType !is ConeTypeParameterType)) {
                val intersection =
                    typeSystemContext.intersectTypes(typeParameters[index].resolvedBounds.map { it.coneType })
                val upperBound = substitutor.substituteOrSelf(intersection)
                if (!AbstractTypeChecker.isSubtypeOf(
                        typeSystemContext,
                        argumentType,
                        upperBound,
                        stubTypesEqualToAnything = true
                    )
                ) {
                    if (isReportExpansionError && argumentTypeRef == null) {
                        reporter.reportOn(
                            argumentSource ?: fallbackSource, typealiasDiagnostic, upperBound, argumentType
                        )
                    } else {
                        val extraMessage =
                            if (upperBound.unwrapToSimpleTypeUsingLowerBound() is ConeCapturedType) "Consider removing the explicit type arguments" else ""
                        when {
                            !isInsideTypeOperatorOrParameterBounds -> reporter.reportOn(
                                argumentSource ?: fallbackSource, regularDiagnostic,
                                upperBound, argumentType, extraMessage
                            )
                            else -> reporter.reportOn(
                                argumentSource ?: fallbackSource,
                                FirErrors.UPPER_BOUND_VIOLATED_IN_TYPE_OPERATOR_OR_PARAMETER_BOUNDS,
                                upperBound, argumentType, extraMessage,
                            )
                        }
                    }
                } else {
                    for (additionalUpperBoundsProvider in additionalUpperBoundsProviders) {
                        // Only check if the original check was successful to prevent duplicate diagnostics
                        val reported = reportUpperBoundViolationWarningIfNecessary(
                            additionalUpperBoundsProvider,
                            argumentType,
                            upperBound,
                            typeSystemContext,
                            isReportExpansionError,
                            argumentTypeRef,
                            argumentSource ?: fallbackSource
                        )
                        if (reported) break
                    }
                }
            }

            if (argumentType is ConeClassLikeType) {
                checkUpperBoundViolated(argumentTypeRef, argumentType, isIgnoreTypeParameters, fallbackSource)
            }
        }
    }
}

fun checkUpperBoundViolatedNoReport(
    typeParameters: List<FirTypeParameterSymbol>,
    typeArguments: List<ConeTypeProjection>,
    session: FirSession,
): Boolean {
    val substitution = typeParameters.zip(typeArguments).toMap()
    val substitutor = FE10LikeConeSubstitutor(substitution, session)

    val count = minOf(typeParameters.size, typeArguments.size)
    val typeSystemContext = session.typeContext

    for (index in 0 until count) {
        val argument = typeArguments[index]
        val argumentType = argument.type
        if (argumentType != null) {
            if (argumentType.typeArguments.isEmpty() && argumentType !is ConeTypeParameterType) {
                val intersection =
                    typeSystemContext.intersectTypes(typeParameters[index].resolvedBounds.map { it.coneType })
                val upperBound = substitutor.substituteOrSelf(intersection)
                if (!AbstractTypeChecker.isSubtypeOf(
                        typeSystemContext,
                        argumentType,
                        upperBound,
                        stubTypesEqualToAnything = true
                    )
                ) {
                    return true
                }
            }
        }
    }
    return false
}

/**
 * @returns true if the diagnostic was reported
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportUpperBoundViolationWarningIfNecessary(
    additionalUpperBoundsProvider: FirPlatformUpperBoundsProvider,
    argumentType: ConeKotlinType,
    upperBound: ConeKotlinType,
    typeSystemContext: ConeInferenceContext,
    isReportExpansionError: Boolean,
    argumentTypeRef: FirTypeRef?,
    argumentSource: KtSourceElement?,
): Boolean {
    val additionalUpperBound = additionalUpperBoundsProvider.getAdditionalUpperBound(upperBound) ?: return false

    /**
     * While [LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible]
     * is here, to obtain original explicit type arguments, we need to look into special attribute.
     * TODO: Get rid of this unwrapping once [LanguageFeature.DontMakeExplicitJavaTypeArgumentsFlexible] is removed
     */
    val properArgumentType =
        argumentType.attributes.explicitTypeArgumentIfMadeFlexibleSynthetically?.coneType ?: argumentType

    if (!AbstractTypeChecker.isSubtypeOf(
            typeSystemContext,
            properArgumentType,
            additionalUpperBound,
            stubTypesEqualToAnything = true
        )
    ) {
        val factory = when {
            isReportExpansionError && argumentTypeRef == null -> additionalUpperBoundsProvider.diagnosticForTypeAlias
            else -> additionalUpperBoundsProvider.diagnostic
        }
        reporter.reportOn(argumentSource, factory, upperBound, properArgumentType)
        return true
    }
    return false
}

fun ConeClassLikeType.fullyExpandedTypeWithSource(
    typeRef: FirTypeRef,
    useSiteSession: FirSession,
): ConeClassLikeType? {
    val typeRefAndSourcesForArguments = extractArgumentsTypeRefAndSource(typeRef) ?: return null

    // Add source information to arguments of non-expanded type, which is preserved during expansion.
    val typeArguments = typeArguments.mapIndexed { i, projection ->
        // typeRefAndSourcesForArguments can have fewer elements than there are type arguments
        // because in FIR, inner types of generic outer types have the generic arguments of the outer type added to the end of their list
        // of type arguments but there is no source for them.
        val source = typeRefAndSourcesForArguments.elementAtOrNull(i) ?: return@mapIndexed projection
        projection.withSource(source)
    }.toTypedArray()

    return withArguments(typeArguments).fullyExpandedType(useSiteSession)
}

private class SourceAttribute(private val data: FirTypeRefSource) : ConeAttribute<SourceAttribute>() {
    val source: KtSourceElement? get() = data.source
    val typeRef: FirTypeRef? get() = data.typeRef

    override fun union(other: SourceAttribute?): SourceAttribute = other ?: this
    override fun intersect(other: SourceAttribute?): SourceAttribute = other ?: this
    override fun add(other: SourceAttribute?): SourceAttribute = other ?: this

    override fun isSubtypeOf(other: SourceAttribute?): Boolean = true

    override fun toString() = "SourceAttribute: $data"

    override val key: KClass<out SourceAttribute>
        get() = SourceAttribute::class
    override val keepInInferredDeclarationType: Boolean
        get() = false
}

private val ConeAttributes.sourceAttribute: SourceAttribute? by ConeAttributes.attributeAccessor()

fun ConeTypeProjection.withSource(source: FirTypeRefSource?): ConeTypeProjection {
    return when {
        source == null || this !is ConeKotlinTypeProjection -> this
        else -> {
            // Prefer existing source information.
            val attributes = ConeAttributes.create(listOf(SourceAttribute(source))).add(type.attributes)
            replaceType(type.withAttributes(attributes))
        }
    }
}

abstract class FirPlatformUpperBoundsProvider : FirComposableSessionComponent<FirPlatformUpperBoundsProvider> {
    abstract val diagnostic: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType>
    abstract val diagnosticForTypeAlias: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType>

    abstract fun getAdditionalUpperBound(coneKotlinType: ConeKotlinType): ConeKotlinType?

    /**
     * Shouldn't be accessed directly.
     */
    class Composed(
        override val components: List<FirPlatformUpperBoundsProvider>
    ) : FirPlatformUpperBoundsProvider(), FirComposableSessionComponent.Composed<FirPlatformUpperBoundsProvider> {
        override val diagnostic: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType>
            get() = shouldNotBeCalled()
        override val diagnosticForTypeAlias: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType>
            get() = shouldNotBeCalled()

        override fun getAdditionalUpperBound(coneKotlinType: ConeKotlinType): ConeKotlinType {
            shouldNotBeCalled()
        }
    }

    @SessionConfiguration
    override fun createComposed(components: List<FirPlatformUpperBoundsProvider>): Composed {
        return Composed(components)
    }
}

private val FirSession.platformUpperBoundsProvider: FirPlatformUpperBoundsProvider? by FirSession.nullableSessionComponentAccessor()
val FirSession.platformUpperBoundsProviders: List<FirPlatformUpperBoundsProvider>
    get() = when (val component = platformUpperBoundsProvider) {
        null -> emptyList()
        is FirPlatformUpperBoundsProvider.Composed -> component.components
        else -> listOf(component)
    }

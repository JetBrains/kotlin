/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
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
import kotlin.reflect.KClass

/**
 * Recursively analyzes type parameters and reports the diagnostic on the given source calculated using typeRef
 */
fun checkUpperBoundViolated(
    typeRef: FirTypeRef?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isIgnoreTypeParameters: Boolean = false
) {
    val type = typeRef?.coneType?.lowerBoundIfFlexible() as? ConeClassLikeType ?: return
    checkUpperBoundViolated(typeRef, type, context, reporter, isIgnoreTypeParameters)
}

private fun checkUpperBoundViolated(
    typeRef: FirTypeRef?,
    notExpandedType: ConeClassLikeType,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isIgnoreTypeParameters: Boolean = false,
) {
    // If we have FirTypeRef information, add KtSourceElement information to each argument of the type and fully expand.
    val type = if (typeRef != null) {
        (notExpandedType.abbreviatedTypeOrSelf as? ConeClassLikeType)
            ?.fullyExpandedTypeWithSource(typeRef, context.session)
            // Add fallback source information to arguments of the expanded type.
            ?.withArguments { it.withSource(FirTypeRefSource(null, typeRef.source)) }
            ?: return
    } else {
        notExpandedType.fullyExpandedType(context.session)
    }

    if (type.typeArguments.isEmpty()) return

    val prototypeClassSymbol = type.lookupTag.toRegularClassSymbol(context.session) ?: return

    val typeParameterSymbols = prototypeClassSymbol.typeParameterSymbols

    if (typeParameterSymbols.isEmpty()) {
        return
    }

    val substitution = typeParameterSymbols.zip(type.typeArguments).toMap()
    val substitutor = FE10LikeConeSubstitutor(substitution, context.session)

    return checkUpperBoundViolated(
        context, reporter, typeParameterSymbols, type.typeArguments.toList(), substitutor,
        isReportExpansionError = true, isIgnoreTypeParameters,
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

fun checkUpperBoundViolated(
    context: CheckerContext,
    reporter: DiagnosticReporter,
    typeParameters: List<FirTypeParameterSymbol>,
    typeArguments: List<ConeTypeProjection>,
    substitutor: ConeSubstitutor,
    isReportExpansionError: Boolean = false,
    isIgnoreTypeParameters: Boolean = false,
) {
    val count = minOf(typeParameters.size, typeArguments.size)
    val typeSystemContext = context.session.typeContext
    val additionalUpperBoundsProvider = context.session.platformUpperBoundsProvider

    for (index in 0 until count) {
        val argument = typeArguments[index]
        val argumentType = argument.type
        val sourceAttribute = argumentType?.attributes?.sourceAttribute
        val argumentTypeRef = sourceAttribute?.typeRef
        val argumentSource = sourceAttribute?.source

        if (argumentType != null && isExplicitTypeArgumentSource(argumentSource)) {
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
                            argumentSource, FirErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION, upperBound, argumentType, context
                        )
                    } else {
                        val extraMessage = if (upperBound.unwrapToSimpleTypeUsingLowerBound() is ConeCapturedType) "Consider removing the explicit type arguments" else ""
                        reporter.reportOn(
                            argumentSource, FirErrors.UPPER_BOUND_VIOLATED,
                            upperBound, argumentType, extraMessage, context
                        )
                    }
                } else {
                    // Only check if the original check was successful to prevent duplicate diagnostics
                    reportUpperBoundViolationWarningIfNecessary(
                        additionalUpperBoundsProvider,
                        argumentType,
                        upperBound,
                        context,
                        typeSystemContext,
                        reporter,
                        isReportExpansionError,
                        argumentTypeRef,
                        argumentSource
                    )
                }
            }

            if (argumentType is ConeClassLikeType) {
                checkUpperBoundViolated(argumentTypeRef, argumentType, context, reporter, isIgnoreTypeParameters)
            }
        }
    }
}

private fun reportUpperBoundViolationWarningIfNecessary(
    additionalUpperBoundsProvider: FirPlatformUpperBoundsProvider?,
    argumentType: ConeKotlinType,
    upperBound: ConeKotlinType,
    context: CheckerContext,
    typeSystemContext: ConeInferenceContext,
    reporter: DiagnosticReporter,
    isReportExpansionError: Boolean,
    argumentTypeRef: FirTypeRef?,
    argumentSource: KtSourceElement?,
) {
    if (additionalUpperBoundsProvider == null) return
    val additionalUpperBound = additionalUpperBoundsProvider.getAdditionalUpperBound(upperBound) ?: return
    // While [org.jetbrains.kotlin.fir.resolve.calls.CreateFreshTypeVariableSubstitutorStage.getTypePreservingFlexibilityWrtTypeVariable]
    // is here, to obtain original explicit type arguments, we need to look into special attribute.
    // TODO: Get rid of this unwrapping once KT-59138 is fixed and the relevant feature for disabling it will be removed
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
        reporter.reportOn(argumentSource, factory, upperBound, properArgumentType, context)
    }
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

interface FirPlatformUpperBoundsProvider : FirSessionComponent {
    val diagnostic: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType>
    val diagnosticForTypeAlias: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType>

    fun getAdditionalUpperBound(coneKotlinType: ConeKotlinType): ConeKotlinType?
}

val FirSession.platformUpperBoundsProvider: FirPlatformUpperBoundsProvider? by FirSession.nullableSessionComponentAccessor()

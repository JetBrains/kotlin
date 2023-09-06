/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.withCombinedAttributesFrom
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
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
    val type = typeRef?.coneTypeSafe<ConeClassLikeType>() ?: return
    checkUpperBoundViolated(typeRef, type, context, reporter, isIgnoreTypeParameters)
}

private fun checkUpperBoundViolated(
    typeRef: FirTypeRef?,
    notExpandedType: ConeClassLikeType,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isIgnoreTypeParameters: Boolean = false,
) {
    if (notExpandedType.typeArguments.isEmpty()) return

    // If we have FirTypeRef information, add KtSourceElement information to each argument of the type and fully expand.
    val type = if (typeRef != null) {
        notExpandedType.fullyExpandedTypeWithSource(typeRef, context.session)
            // Add fallback source information to arguments of the expanded type.
            ?.withArguments { it.withSource(FirTypeRefSource(null, typeRef.source)) }
            ?: return
    } else {
        notExpandedType
    }

    val prototypeClassSymbol = type.lookupTag.toSymbol(context.session) as? FirRegularClassSymbol ?: return

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

/**
 * This substitutor replaces type projections with type of this projection
 * Star projections are replaced with Any?
 */
internal class FE10LikeConeSubstitutor(
    private val substitution: Map<FirTypeParameterSymbol, ConeTypeProjection>,
    useSiteSession: FirSession
) : AbstractConeSubstitutor(useSiteSession.typeContext) {
    constructor(
        typeParameters: List<FirTypeParameterSymbol>,
        typeArguments: List<ConeTypeProjection>,
        useSiteSession: FirSession
    ) : this(typeParameters.zip(typeArguments).toMap(), useSiteSession)

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeTypeParameterType) return null
        val projection = substitution[type.lookupTag.symbol] ?: return null

        if (projection.isStarProjection) {
            return StandardClassIds.Any.constructClassLikeType(emptyArray(), isNullable = true).withProjection(projection)
        }

        val result =
            projection.type!!.updateNullabilityIfNeeded(type)?.withCombinedAttributesFrom(type)
                ?: return null

        return result.withProjection(projection)
    }

    private fun ConeKotlinType.withProjection(projection: ConeTypeProjection): ConeKotlinType {
        if (projection.kind == ProjectionKind.INVARIANT) return this
        return withAttributes(ConeAttributes.create(listOf(OriginalProjectionTypeAttribute(projection))))
    }

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val substitutedProjection = super.substituteArgument(projection, index) ?: return null
        if (substitutedProjection.isStarProjection) return null

        val type = substitutedProjection.type!!

        val projectionFromType = type.attributes.originalProjection?.data ?: type
        val projectionKindFromType = projectionFromType.kind

        if (projectionKindFromType == ProjectionKind.STAR) return ConeStarProjection

        if (projectionKindFromType == ProjectionKind.INVARIANT || projectionKindFromType == projection.kind) {
            return substitutedProjection
        }

        if (projection.kind == ProjectionKind.INVARIANT) {
            return wrapProjection(projectionFromType, type)
        }

        return ConeStarProjection
    }
}

private class OriginalProjectionTypeAttribute(val data: ConeTypeProjection) : ConeAttribute<OriginalProjectionTypeAttribute>() {
    override fun union(other: OriginalProjectionTypeAttribute?): OriginalProjectionTypeAttribute = other ?: this
    override fun intersect(other: OriginalProjectionTypeAttribute?): OriginalProjectionTypeAttribute = other ?: this
    override fun add(other: OriginalProjectionTypeAttribute?): OriginalProjectionTypeAttribute = other ?: this

    override fun isSubtypeOf(other: OriginalProjectionTypeAttribute?): Boolean {
        return true
    }

    override fun toString() = "OriginalProjectionTypeAttribute: $data"

    override val key: KClass<out OriginalProjectionTypeAttribute>
        get() = OriginalProjectionTypeAttribute::class
}

private val ConeAttributes.originalProjection: OriginalProjectionTypeAttribute? by ConeAttributes.attributeAccessor()

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

    for (index in 0 until count) {
        val argument = typeArguments[index]
        val argumentType = argument.type
        val sourceAttribute = argumentType?.attributes?.sourceAttribute
        val argumentTypeRef = sourceAttribute?.typeRef
        val argumentSource = sourceAttribute?.source

        if (argumentType != null && argumentSource != null) {
            if (!isIgnoreTypeParameters || (argumentType.typeArguments.isEmpty() && argumentType !is ConeTypeParameterType)) {
                val intersection =
                    typeSystemContext.intersectTypes(typeParameters[index].resolvedBounds.map { it.coneType }) as? ConeKotlinType
                if (intersection != null) {
                    val upperBound = substitutor.substituteOrSelf(intersection)
                    if (!AbstractTypeChecker.isSubtypeOf(
                            typeSystemContext,
                            argumentType,
                            upperBound,
                            stubTypesEqualToAnything = true
                        )
                    ) {
                        val factory = when {
                            isReportExpansionError && argumentTypeRef == null -> FirErrors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION
                            else -> FirErrors.UPPER_BOUND_VIOLATED
                        }
                        reporter.reportOn(argumentSource, factory, upperBound, argumentType.type, context)
                    }
                }
            }

            if (argumentType is ConeClassLikeType) {
                checkUpperBoundViolated(argumentTypeRef, argumentType, context, reporter, isIgnoreTypeParameters)
            }
        }
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

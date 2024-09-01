/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.wrapProjection
import org.jetbrains.kotlin.fir.resolve.withCombinedAttributesFrom
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isStarProjection
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withAttributes
import org.jetbrains.kotlin.name.StandardClassIds
import kotlin.reflect.KClass

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

        val result = projection.type!!.updateNullabilityIfNeeded(type).withCombinedAttributesFrom(type)

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

private val ConeAttributes.originalProjection: OriginalProjectionTypeAttribute? by ConeAttributes.attributeAccessor()

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
    override val keepInInferredDeclarationType: Boolean
        get() = false
}
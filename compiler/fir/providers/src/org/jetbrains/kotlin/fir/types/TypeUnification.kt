/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

/**
 * @return false does only mean that there were conflicted values for some type parameter. In all other cases, it returns true.
 * "fail" result in the comments below means that we can't infer anything meaningful in that branch of unification.
 * See more at org.jetbrains.kotlin.types.TypeUnifier.doUnify.
 * NB: "Failed@ result of UnificationResultImpl is effectively unused in production.
 */
fun FirSession.doUnify(
    originalTypeProjection: ConeTypeProjection,
    typeWithParametersProjection: ConeTypeProjection,
    targetTypeParameters: Set<FirTypeParameterSymbol>,
    result: MutableMap<FirTypeParameterSymbol, ConeTypeProjection>,
): Boolean {
    val originalType = originalTypeProjection.type?.lowerBoundIfFlexible()?.fullyExpandedType(this)
    val typeWithParameters = typeWithParametersProjection.type?.lowerBoundIfFlexible()?.fullyExpandedType(this)

    if (originalType is ConeIntersectionType) {
        val intersectionResult = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
        for (intersectedType in originalType.intersectedTypes) {
            val localResult = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
            if (!doUnify(intersectedType, typeWithParametersProjection, targetTypeParameters, localResult)) return false
            for ((typeParameter, typeProjection) in localResult) {
                val existingTypeProjection = intersectionResult[typeParameter]
                if (existingTypeProjection == null
                    || (typeProjection is KotlinTypeMarker &&
                            existingTypeProjection is KotlinTypeMarker &&
                            AbstractTypeChecker.isSubtypeOf(typeContext, typeProjection, existingTypeProjection))
                ) {
                    intersectionResult[typeParameter] = typeProjection
                }
            }
        }
        for ((key, value) in intersectionResult) {
            result[key] = value
        }
        return true
    }

    // in Foo ~ in X  =>  Foo ~ X
    if (originalTypeProjection.kind == typeWithParametersProjection.kind &&
        originalTypeProjection.kind != ProjectionKind.INVARIANT && originalTypeProjection.kind != ProjectionKind.STAR) {
        return doUnify(originalType!!, typeWithParameters!!, targetTypeParameters, result)
    }

    // Foo? ~ X?  =>  Foo ~ X
    if (originalType?.nullability == ConeNullability.NULLABLE && typeWithParameters?.nullability == ConeNullability.NULLABLE) {
        return doUnify(
            originalTypeProjection.removeQuestionMark(typeContext),
            typeWithParametersProjection.removeQuestionMark(typeContext),
            targetTypeParameters, result,
        )
    }

    // in Foo ~ out X  => fail
    // in Foo ~ X  =>  may be OK
    if (originalTypeProjection.kind != typeWithParametersProjection.kind && typeWithParametersProjection.kind != ProjectionKind.INVARIANT) {
        return true
    }

    if (typeWithParameters is ConeDefinitelyNotNullType) {
        return doUnify(
            originalTypeProjection,
            typeWithParametersProjection.replaceType(typeWithParameters.original),
            targetTypeParameters, result,
        )
    }

    // Foo ~ X? => fail
    if (
        originalTypeProjection !is ConeStarProjection &&
        originalType?.nullability != ConeNullability.NULLABLE &&
        typeWithParameters?.nullability == ConeNullability.NULLABLE
    ) {
        return true
    }

    // Foo ~ X  =>  x |-> Foo
    // * ~ X => x |-> *
    val typeParameter = (typeWithParameters as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol
    if (typeParameter != null && typeParameter in targetTypeParameters) {
        if (typeParameter in result && result[typeParameter] != originalTypeProjection) return false
        result[typeParameter] = originalTypeProjection
        return true
    }

    // Foo? ~ Foo || in Foo ~ Foo || Foo ~ Bar
    if (originalType?.nullability?.isNullable != typeWithParameters?.nullability?.isNullable) return true
    if (originalTypeProjection.kind != typeWithParametersProjection.kind) return true
    if ((originalType as? ConeLookupTagBasedType)?.lookupTag != (typeWithParameters as? ConeLookupTagBasedType)?.lookupTag) return true
    if (originalType == null || typeWithParameters == null) return true

    // Foo<A> ~ Foo<B, C>
    if (originalType.typeArguments.size != typeWithParameters.typeArguments.size) {
        return true
    }

    // Foo ~ Foo
    if (originalType.typeArguments.isEmpty()) {
        return true
    }

    // Foo<...> ~ Foo<...>
    for ((originalTypeArgument, typeWithParametersArgument) in originalType.typeArguments.zip(typeWithParameters.typeArguments)) {
        if (!doUnify(originalTypeArgument, typeWithParametersArgument, targetTypeParameters, result)) return false
    }

    return true
}

private fun ConeTypeProjection.removeQuestionMark(typeContext: ConeTypeContext): ConeTypeProjection {
    val type = type
    require(type != null && type.nullability.isNullable) {
        "Expected nullable type, got $type"
    }

    return replaceType(type.withNullability(ConeNullability.NOT_NULL, typeContext))
}

private fun ConeTypeProjection.replaceType(newType: ConeKotlinType): ConeTypeProjection =
    when (kind) {
        ProjectionKind.INVARIANT -> newType
        ProjectionKind.IN -> ConeKotlinTypeProjectionIn(newType)
        ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(newType)
        ProjectionKind.STAR -> error("Should not be a star projection")
    }

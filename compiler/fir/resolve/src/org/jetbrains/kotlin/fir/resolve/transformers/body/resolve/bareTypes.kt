/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

fun BodyResolveComponents.computeRepresentativeTypeForBareType(type: ConeClassLikeType, originalType: ConeKotlinType): ConeKotlinType? {
    originalType.lowerBoundIfFlexible().fullyExpandedType(session).let {
        if (it !== originalType) return computeRepresentativeTypeForBareType(type, it)
    }

    if (originalType is ConeIntersectionType) {
        val candidatesFromIntersectedTypes = originalType.intersectedTypes.mapNotNull { computeRepresentativeTypeForBareType(type, it) }
        candidatesFromIntersectedTypes.firstOrNull { it.typeArguments.isNotEmpty() }?.let { return it }
        return candidatesFromIntersectedTypes.firstOrNull()
    }

    val originalClassLookupTag = (originalType as? ConeClassLikeType)?.fullyExpandedType(session)?.lookupTag ?: return null

    val castTypeAlias = type.lookupTag.toSymbol(session)?.fir as? FirTypeAlias
    if (castTypeAlias != null && !canBeUsedAsBareType(castTypeAlias)) return null

    val expandedCastType = type.fullyExpandedType(session)
    val castClass = expandedCastType.lookupTag.toSymbol(session)?.fir as? FirRegularClass ?: return null

    val superTypeWithParameters = with(session.typeContext) {
        val correspondingSupertype = AbstractTypeChecker.findCorrespondingSupertypes(
            newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false),
            castClass.defaultType(), originalClassLookupTag,
        ).firstOrNull() as? ConeClassLikeType ?: return null

        if (originalType.nullability.isNullable)
            correspondingSupertype.withNullability(nullable = true) as ConeClassLikeType
        else
            correspondingSupertype
    }

    val substitution = mutableMapOf<FirTypeParameterRef, ConeTypeProjection>()
    val typeParameters = castClass.typeParameters.mapTo(mutableSetOf()) { it.symbol.fir }
    if (!doUnify(originalType, superTypeWithParameters, typeParameters, substitution)) return null

    val newArguments = castClass.typeParameters.map { substitution[it.symbol.fir] ?: return@computeRepresentativeTypeForBareType null }
    return expandedCastType.withArguments(newArguments.toTypedArray())
}

private fun canBeUsedAsBareType(firTypeAlias: FirTypeAlias): Boolean {
    val typeAliasParameters = firTypeAlias.typeParameters.toSet()
    val usedTypeParameters = mutableSetOf<FirTypeParameter>()

    val expandedType = firTypeAlias.expandedConeType ?: return false
    for (argument in expandedType.typeArguments) {
        if (argument.kind == ProjectionKind.STAR) continue
        if (argument.kind != ProjectionKind.INVARIANT) return false

        val type = argument.type!!
        val typeParameter = (type as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol?.fir ?: return false
        if (typeParameter !in typeAliasParameters || typeParameter in usedTypeParameters) return false

        usedTypeParameters.add(typeParameter)
    }

    return true
}

/**
 * @return false does only mean that there were conflicted values for some type parameter. In all other cases, it returns true.
 * "fail" result in the comments below means that we can't infer anything meaningful in that branch of unification.
 * See more at org.jetbrains.kotlin.types.TypeUnifier.doUnify.
 * NB: "Failed@ result of UnificationResultImpl is effectively unused in production.
 */
private fun BodyResolveComponents.doUnify(
    originalTypeProjection: ConeTypeProjection,
    typeWithParametersProjection: ConeTypeProjection,
    targetTypeParameters: Set<FirTypeParameterRef>,
    result: MutableMap<FirTypeParameterRef, ConeTypeProjection>,
): Boolean {
    val originalType = originalTypeProjection.type
    val typeWithParameters = typeWithParametersProjection.type

    // in Foo ~ in X  =>  Foo ~ X
    if (originalTypeProjection.kind == typeWithParametersProjection.kind &&
        originalTypeProjection.kind != ProjectionKind.INVARIANT && originalTypeProjection.kind != ProjectionKind.STAR) {
        return doUnify(originalType!!, typeWithParameters!!, targetTypeParameters, result)
    }

    // Foo? ~ X?  =>  Foo ~ X
    if (originalType?.nullability == ConeNullability.NULLABLE && typeWithParameters?.nullability == ConeNullability.NULLABLE) {
        return doUnify(
            originalTypeProjection.removeQuestionMark(session.typeContext),
            typeWithParametersProjection.removeQuestionMark(session.typeContext),
            targetTypeParameters, result,
        )
    }

    // in Foo ~ out X  => fail
    // in Foo ~ X  =>  may be OK
    if (originalTypeProjection.kind != typeWithParametersProjection.kind && typeWithParametersProjection.kind != ProjectionKind.INVARIANT) {
        return true
    }

    if (typeWithParameters is ConeFlexibleType) {
        return doUnify(
            originalTypeProjection,
            typeWithParametersProjection.replaceType(typeWithParameters.lowerBound),
            targetTypeParameters, result,
        )
    }

    // Foo ~ X? => fail
    if (originalType?.nullability != ConeNullability.NULLABLE && typeWithParameters?.nullability == ConeNullability.NULLABLE) {
        return true
    }

    // Foo ~ X  =>  x |-> Foo
    // * ~ X => x |-> *
    val typeParameter = (typeWithParameters as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol?.fir
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

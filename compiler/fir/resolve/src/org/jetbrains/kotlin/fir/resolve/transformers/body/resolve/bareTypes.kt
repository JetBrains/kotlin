/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

fun BodyResolveComponents.computeRepresentativeTypeForBareType(type: ConeClassLikeType, originalType: ConeKotlinType): ConeKotlinType? {
    originalType.lowerBoundIfFlexible().fullyExpandedType(session).let {
        if (it !== originalType) return computeRepresentativeTypeForBareType(type, it)
    }

    if (originalType is ConeIntersectionType) {
        val candidatesFromIntersectedTypes = originalType.intersectedTypes.mapNotNull { computeRepresentativeTypeForBareType(type, it) }
        candidatesFromIntersectedTypes.firstOrNull { it.typeArguments.isNotEmpty() }?.let { return it }
        return candidatesFromIntersectedTypes.firstOrNull()
    }

    session.typeApproximator.approximateToSuperType(
        originalType,
        TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
    )?.let {
        return computeRepresentativeTypeForBareType(type, it)
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

    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
    val typeParameters = castClass.typeParameters.mapTo(mutableSetOf()) { it.symbol }
    if (!session.doUnify(originalType, superTypeWithParameters, typeParameters, substitution)) return null

    val newArguments = castClass.typeParameters.map { substitution[it.symbol] ?: return@computeRepresentativeTypeForBareType null }
    return expandedCastType.withArguments(newArguments.toTypedArray())
}

private fun canBeUsedAsBareType(firTypeAlias: FirTypeAlias): Boolean {
    firTypeAlias.lazyResolveToPhase(FirResolvePhase.TYPES)

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


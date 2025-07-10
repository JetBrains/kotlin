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
    resultCe: MutableMap<FirTypeParameterSymbol, CEType>,
): Boolean {
    val originalType = originalTypeProjection.type?.lowerBoundIfFlexible()?.fullyExpandedType(this)
    val typeWithParameters = typeWithParametersProjection.type?.lowerBoundIfFlexible()?.fullyExpandedType(this)

    if (typeWithParameters is ConeErrorType) {
        return true // Return true to avoid loosing `result` substitution
    }

    if (originalType is ConeIntersectionType) {
        val intersectionResult = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
        for (intersectedType in originalType.intersectedTypes) {
            val localResult = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
            // TODO: RE: MID: We should replicate the behaviour for value types
            if (!doUnify(intersectedType, typeWithParametersProjection, targetTypeParameters, localResult, resultCe)) return false
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

    if (typeWithParameters is ConeErrorUnionType) {
        val valueType: ConeTypeProjection
        val errorType: CEType
        if (originalType is ConeErrorUnionType) {
            valueType = originalType.valueType
            errorType = originalType.errorType
        } else {
            valueType = originalType ?: originalTypeProjection
            errorType = CEBotType
        }
        if (!doUnify(valueType, typeWithParameters.valueType, targetTypeParameters, result, resultCe)) return false
        if (!doUnifyCe(errorType, typeWithParameters.errorType, targetTypeParameters, resultCe)) return false
        return true
    }

    // in Foo ~ in X  =>  Foo ~ X
    if (originalTypeProjection.kind == typeWithParametersProjection.kind &&
        originalTypeProjection.kind != ProjectionKind.INVARIANT && originalTypeProjection.kind != ProjectionKind.STAR
    ) {
        return doUnify(originalType!!, typeWithParameters!!, targetTypeParameters, result, resultCe)
    }

    // Foo? ~ X?  =>  Foo ~ X
    if (originalType?.isMarkedNullable == true && typeWithParameters?.isMarkedNullable == true) {
        return doUnify(
            originalTypeProjection.removeQuestionMark(this),
            typeWithParametersProjection.removeQuestionMark(this),
            targetTypeParameters, result, resultCe
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
            targetTypeParameters, result, resultCe
        )
    }

    // Foo ~ X? => fail
    if (
        originalTypeProjection !is ConeStarProjection &&
        originalType?.isMarkedNullable != true &&
        typeWithParameters?.isMarkedNullable == true
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
    if (originalType?.isMarkedNullable != typeWithParameters?.isMarkedNullable) return true
    if (originalTypeProjection.kind != typeWithParametersProjection.kind) return true
    if (originalType?.lookupTagIfAny != typeWithParameters?.lookupTagIfAny) return true
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
        if (!doUnify(originalTypeArgument, typeWithParametersArgument, targetTypeParameters, result, resultCe)) return false
    }

    return true
}

private fun ConeTypeProjection.removeQuestionMark(session: FirSession): ConeTypeProjection {
    val type = type?.fullyExpandedType(session)
    require(type != null && type.isMarkedNullable) {
        "Expected nullable type, got $type"
    }

    return replaceType(type.withNullability(nullable = false, session.typeContext))
}

private fun ConeTypeProjection.replaceType(newType: ConeKotlinType): ConeTypeProjection =
    when (kind) {
        ProjectionKind.INVARIANT -> newType
        ProjectionKind.IN -> ConeKotlinTypeProjectionIn(newType)
        ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(newType)
        ProjectionKind.STAR -> error("Should not be a star projection")
    }

// TODO: RE: MID: I was not fiully sure in the expected behaviour of this function
private fun FirSession.doUnifyCe(
    originalTypeProjection: CEType,
    typeWithParametersProjection: CEType,
    targetTypeParameters: Set<FirTypeParameterSymbol>,
    resultCe: MutableMap<FirTypeParameterSymbol, CEType>,
): Boolean {
    val originalAtoms = when (originalTypeProjection) {
        is CEUnionType -> originalTypeProjection.types.toMutableSet()
        else -> mutableSetOf(originalTypeProjection)
    }
    val typeWithParametersAtoms = when (typeWithParametersProjection) {
        is CEUnionType -> typeWithParametersProjection.types
        else -> listOf(typeWithParametersProjection)
    }.filter {
        if (it in originalAtoms) {
            originalAtoms.remove(it)
            false
        } else true
    }

    if (originalAtoms.isEmpty() && typeWithParametersAtoms.isEmpty()) {
        return true
    }

//    val parameter = if (typeWithParametersAtoms.size == 1) {
//        val singleAtom = typeWithParametersAtoms.single()
//        if (singleAtom !is CETypeParameterType) {
//            return false
//        }
//        val parameterSymbol = singleAtom.lookupTag.typeParameterSymbol
//        if (parameterSymbol !in targetTypeParameters) {
//            return false
//        }
//        parameterSymbol
//    } else {
//        return false
//    }

    val parameterType = typeWithParametersAtoms.find { it is CETypeParameterType } ?: return true
    val parameter = (parameterType as CETypeParameterType).lookupTag.typeParameterSymbol
    if (parameter !in targetTypeParameters) return true

    val mappedType = if (originalAtoms.isEmpty()) {
        CEBotType
    } else {
        CEUnionType.create(originalAtoms.toList())
    }
    val existingMappedType = resultCe[parameter]
    return when {
        existingMappedType == null -> {
            resultCe[parameter] = mappedType
            true
        }
        existingMappedType.isEqualTo(mappedType) -> true
        else -> false
    }
}

private fun CEType.isEqualTo(other: CEType): Boolean {
    return when (this) {
        is CEBotType -> other is CEBotType
        is CETopType -> other is CETopType
        is CELookupTagBasedType -> {
            if (other !is CELookupTagBasedType) return false
            lookupTag == other.lookupTag
        }
        is CETypeVariableType -> {
            if (other !is CETypeVariableType) return false
            typeConstructor == other.typeConstructor
        }
        is CEUnionType -> {
            if (other !is CEUnionType) return false
            val otherSet = other.types.toMutableSet()
            for (type in types) {
                var found = false
                for (otherType in otherSet) {
                    if (type.isEqualTo(otherType)) {
                        otherSet.remove(otherType)
                        found = true
                        break
                    }
                }
                if (!found) return false
            }
            otherSet.isEmpty()
        }
    }
}

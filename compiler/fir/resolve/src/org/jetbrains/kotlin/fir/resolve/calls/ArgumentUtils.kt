/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.TypeSystemCommonSuperTypesContext

internal fun prepareCapturedType(argumentType: ConeKotlinType, context: ResolutionContext): ConeKotlinType {
    if (argumentType.isRaw()) return argumentType
    return context.typeContext.captureFromExpression(argumentType.fullyExpandedType(context.session)) ?: argumentType
}

fun FirExpression.getExpectedType(
    session: FirSession,
    parameter: FirValueParameter/*, languageVersionSettings: LanguageVersionSettings*/
): ConeKotlinType {
    val shouldUnwrapVarargType = when (this) {
        is FirSpreadArgumentExpression, is FirNamedArgumentExpression -> false
        else -> parameter.isVararg
    }

    val expectedType = if (shouldUnwrapVarargType) {
        parameter.returnTypeRef.coneType.varargElementType()
    } else {
        parameter.returnTypeRef.coneType
    }
    if (!session.functionTypeService.hasExtensionKinds()) return expectedType
    return FunctionTypeKindSubstitutor(session).substituteOrSelf(expectedType)
}

/**
 * This class creates a type by recursively substituting function types of a given type if the function types have special function
 * type kinds.
 */
private class FunctionTypeKindSubstitutor(private val session: FirSession) : AbstractConeSubstitutor(session.typeContext) {
    /**
     * Returns a new type that applies the special function type kind to [type] if [type] has a special function type kind.
     */
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeClassLikeType) return null
        val classId = type.classId ?: return null
        return session.functionTypeService.extractSingleExtensionKindForDeserializedConeType(classId, type.customAnnotations)
            ?.let { functionTypeKind ->
                type.createFunctionTypeWithNewKind(session, functionTypeKind) {
                    // When `substituteType()` returns a non-null value, it does not recursively substitute type arguments,
                    // which is problematic for a nested function type kind like `@Composable () -> (@Composable -> Unit)`.
                    // To fix this issue, we manually substitute type arguments here.
                    this.mapIndexed { index, coneTypeProjection -> substituteArgument(coneTypeProjection, index) ?: coneTypeProjection }
                        .toTypedArray()
                }
            }
    }
}

/**
 * interface Inv<T>
 * fun <Y> bar(l: Inv<Y>): Y = ...
 *
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val xr = bar(x)
 * }
 * Here we try to capture from upper bound from type parameter.
 * We replace type of `x` to `Inv<out Int>`(we chose supertype which contains supertype with expectedTypeConstructor) and capture from this type.
 * It is correct, because it is like this code:
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val inv: Inv<out Int> = x
 *      val xr = bar(inv)
 * }
 *
 */
internal fun captureFromTypeParameterUpperBoundIfNeeded(
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType,
    session: FirSession
): ConeKotlinType {
    val expectedTypeClassId = expectedType.upperBoundIfFlexible().classId ?: return argumentType
    val simplifiedArgumentType = argumentType.lowerBoundIfFlexible() as? ConeTypeParameterType ?: return argumentType
    val context = session.typeContext

    val chosenSupertype = simplifiedArgumentType.collectUpperBounds()
        .singleOrNull { it.hasSupertypeWithGivenClassId(expectedTypeClassId, context) } ?: return argumentType

    val capturedType = context.captureFromExpression(chosenSupertype) ?: return argumentType
    return if (argumentType is ConeDefinitelyNotNullType) {
        ConeDefinitelyNotNullType.create(capturedType, session.typeContext) ?: capturedType
    } else {
        capturedType
    }
}

private fun ConeKotlinType.hasSupertypeWithGivenClassId(classId: ClassId, context: TypeSystemCommonSuperTypesContext): Boolean {
    return with(context) {
        anySuperTypeConstructor {
            val typeConstructor = it.typeConstructor()
            typeConstructor is ConeClassLikeLookupTag && typeConstructor.classId == classId
        }
    }
}

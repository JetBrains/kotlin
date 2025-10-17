/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnknownLambdaParameterTypeDiagnostic
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE

enum class TypeVariableReplacement {
    TypeParameter, ErrorType,
}

fun ConeKotlinType.removeTypeVariableTypes(typeContext: ConeTypeContext, replacement: TypeVariableReplacement): ConeKotlinType {
    val substitutor = TypeVariableTypeRemovingSubstitutor(typeContext, replacement)
    return substitutor.substituteOrSelf(this)
}

private class TypeVariableTypeRemovingSubstitutor(typeContext: ConeTypeContext, private val replacement: TypeVariableReplacement) : AbstractConeSubstitutor(typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? = when (type) {
        is ConeTypeVariableType -> convertTypeVariableType(type)
        else -> null
    }

    private fun convertTypeVariableType(type: ConeTypeVariableType): ConeKotlinType {
        val originalTypeParameter = type.typeConstructor.originalTypeParameter
        if (originalTypeParameter != null) {
            check(originalTypeParameter is ConeTypeParameterLookupTag)
            val typeParameterType = ConeTypeParameterTypeImpl(originalTypeParameter, type.isMarkedNullable, type.attributes)
            return if (replacement == TypeVariableReplacement.ErrorType) {
                ConeErrorType(
                    ConeCannotInferTypeParameterType(typeParameter = originalTypeParameter.typeParameterSymbol),
                    isUninferredParameter = true,
                    delegatedType = typeParameterType,
                )
            } else {
                typeParameterType
            }
        }
        return ConeErrorType(
            ConeUnknownLambdaParameterTypeDiagnostic(isReturnType = type.typeConstructor.debugName == TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
        )
    }

    override fun toString(): String {
        return "{<Type variable> -> <Error type>}"
    }
}

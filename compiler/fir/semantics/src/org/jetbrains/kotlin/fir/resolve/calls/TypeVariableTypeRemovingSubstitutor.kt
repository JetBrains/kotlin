/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnknownLambdaParameterTypeDiagnostic
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

fun ConeKotlinType.removeTypeVariableTypes(typeContext: ConeTypeContext): ConeKotlinType {
    val substitutor = TypeVariableTypeRemovingSubstitutor(typeContext)
    return substitutor.substituteOrSelf(this)
}

private class TypeVariableTypeRemovingSubstitutor(typeContext: ConeTypeContext) : AbstractConeSubstitutor(typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? = when (type) {
        is ConeTypeVariableType -> convertTypeVariableType(type)
        else -> null
    }

    private fun convertTypeVariableType(type: ConeTypeVariableType): ConeKotlinType {
        val originalTypeParameter = type.lookupTag.originalTypeParameter
        if (originalTypeParameter != null) {
            check(originalTypeParameter is ConeTypeParameterLookupTag)
            return ConeTypeParameterTypeImpl(originalTypeParameter, type.isNullable, type.attributes)
        }
        return ConeErrorType(ConeUnknownLambdaParameterTypeDiagnostic())
    }

    override fun toString(): String {
        return "{<Type variable> -> <Error type>}"
    }
}

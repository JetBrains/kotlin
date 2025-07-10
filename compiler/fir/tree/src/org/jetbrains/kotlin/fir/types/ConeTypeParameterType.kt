/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag

abstract class ConeTypeParameterType : ConeLookupTagBasedType() {
    abstract override val lookupTag: ConeTypeParameterLookupTag
}

data class CETypeParameterType(
    override val lookupTag: ConeTypeParameterLookupTag,
) : CELookupTagBasedType()

fun ConeKotlinType.lookupTagIfTypeParameter(): ConeTypeParameterLookupTag? {
    if (this is ConeTypeParameterType) return lookupTag
    if (this !is ConeErrorUnionType) return null
    val valueType = valueType
    val errorType = errorType
    if (valueType is ConeTypeParameterType && errorType is CETypeParameterType && errorType.lookupTag == valueType.lookupTag) return valueType.lookupTag
    return null
}

fun ConeKotlinType.lookupTagIfTypeParameterIgnoringDnn(): ConeTypeParameterLookupTag? {
    fun ConeKotlinType.unwrapIfDnn(): ConeKotlinType =
        if (this is ConeDefinitelyNotNullType) original else this
    (this.unwrapIfDnn() as? ConeTypeParameterType)?.lookupTag?.let { return it }
    if (this !is ConeErrorUnionType) return null
    val valueType = valueType.unwrapIfDnn()
    val errorType = errorType
    if (valueType is ConeTypeParameterType && errorType is CETypeParameterType && errorType.lookupTag == valueType.lookupTag) return valueType.lookupTag
    return null
}

fun ConeKotlinType.isTypeParameter(): Boolean = lookupTagIfTypeParameter() != null

fun ConeKotlinType.typeConstructorIfTypeVariableType(): ConeTypeVariableTypeConstructor? {
    if (this is ConeTypeVariableType) return typeConstructor
    if (this !is ConeErrorUnionType) return null
    val valueType = valueType
    val errorType = errorType
    if (valueType is ConeTypeVariableType && errorType is CETypeVariableType && errorType.typeConstructor == valueType.typeConstructor) return valueType.typeConstructor
    return null
}

fun ConeKotlinType.isTypeVariableType(): Boolean = typeConstructorIfTypeVariableType() != null

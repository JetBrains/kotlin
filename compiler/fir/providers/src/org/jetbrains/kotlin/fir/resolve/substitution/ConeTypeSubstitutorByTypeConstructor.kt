/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.resolve.withCombinedAttributesFrom
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.typeConstructor

fun createTypeSubstitutorByTypeConstructor(
    map: Map<TypeConstructorMarker, ConeKotlinType>,
    context: ConeTypeContext,
    approximateIntegerLiterals: Boolean
): ConeSubstitutor {
    if (map.isEmpty()) return ConeSubstitutor.Empty
    return ConeTypeSubstitutorByTypeConstructor(map, context, approximateIntegerLiterals)
}

private class ConeTypeSubstitutorByTypeConstructor(
    private val map: Map<TypeConstructorMarker, ConeKotlinType>,
    typeContext: ConeTypeContext,
    private val approximateIntegerLiterals: Boolean
) : AbstractConeSubstitutor(typeContext), TypeSubstitutorMarker {

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeLookupTagBasedType && type !is ConeStubType && type !is ConeTypeVariableType) return null
        val new = map[type.typeConstructor(typeContext)] ?: return null
        val approximatedIntegerLiteralType = if (approximateIntegerLiterals) new.approximateIntegerLiteralType() else new
        return approximatedIntegerLiteralType.updateNullabilityIfNeeded(type).withCombinedAttributesFrom(type)
    }

    override fun toString(): String {
        return map.entries.joinToString(prefix = "{", postfix = "}", separator = " | ") { (constructor, type) ->
            "$constructor -> ${type.renderForDebugging()}"
        }
    }
}

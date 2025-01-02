/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.withCombinedAttributesFrom
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.typeContext

fun substitutorByTypeParameterErasure(
    useSiteSession: FirSession,
): ConeSubstitutor {
    return TypeParameterErasureSubstitutor.create(useSiteSession)
}

class TypeParameterErasureSubstitutor private constructor(
    useSiteSession: FirSession
) : AbstractConeSubstitutor(useSiteSession.typeContext) {
    companion object {
        fun create(
            useSiteSession: FirSession
        ): ConeSubstitutor {
            return TypeParameterErasureSubstitutor(useSiteSession)
        }
    }

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type is ConeTypeParameterType)
            return typeContext.session.builtinTypes.anyType.coneType.updateNullabilityIfNeeded(type).withCombinedAttributesFrom(type)
        return null
    }
}

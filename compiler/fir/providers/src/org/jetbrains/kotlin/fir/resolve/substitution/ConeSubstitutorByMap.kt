/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.withCombinedAttributesFrom
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

fun substitutorByMap(substitution: Map<FirTypeParameterSymbol, ConeKotlinType>, useSiteSession: FirSession): ConeSubstitutor {
    return ConeSubstitutorByMap.create(substitution, useSiteSession, allowIdenticalSubstitution = false)
}

class ConeSubstitutorByMap private constructor(
    // Used only for sake of optimizations at org.jetbrains.kotlin.analysis.api.fir.types.KtFirMapBackedSubstitutor
    val substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
    private val useSiteSession: FirSession
) : AbstractConeSubstitutor(useSiteSession.typeContext) {
    companion object {
        fun create(
            substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
            useSiteSession: FirSession,
            allowIdenticalSubstitution: Boolean = true,
        ): ConeSubstitutor {
            if (substitution.isEmpty()) return Empty

            if (!allowIdenticalSubstitution) {
                // If all arguments match parameters, then substitutor isn't needed
                val substitutionIsIdentical = substitution.all { (parameterSymbol, argumentType) ->
                    (argumentType as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol == parameterSymbol && !argumentType.isMarkedNullable
                }
                if (substitutionIsIdentical) {
                    return Empty
                }
            }
            return ConeSubstitutorByMap(substitution, useSiteSession)
        }
    }

    private val hashCode by lazy(LazyThreadSafetyMode.PUBLICATION) {
        substitution.hashCode()
    }

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeTypeParameterType) return null
        return substitution[type.lookupTag.symbol]?.updateNullabilityIfNeeded(type)
            ?.withCombinedAttributesFrom(type)
            ?: return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeSubstitutorByMap) return false

        if (hashCode != other.hashCode) return false
        if (substitution != other.substitution) return false
        if (useSiteSession != other.useSiteSession) return false

        return true
    }

    override fun hashCode(): Int = hashCode

    override fun toString(): String {
        return substitution.entries.joinToString(prefix = "{", postfix = "}", separator = " | ") { (param, type) ->
            "${param.name} -> ${type.renderForDebugging()}"
        }
    }
}

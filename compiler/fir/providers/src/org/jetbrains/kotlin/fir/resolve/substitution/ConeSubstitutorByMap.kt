/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.withCombinedAttributesFrom
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

/**
 * If [allowIdenticalSubstitution] set to false then for empty and identical substitutions
 *   [ConeSubstitutor.Empty] will be returned
 */
fun substitutorByMap(
    substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
    ceSubstitution: Map<FirTypeParameterSymbol, CEType>,
    useSiteSession: FirSession,
    allowIdenticalSubstitution: Boolean = false,
): ConeSubstitutor {
    return ConeSubstitutorByMap.create(substitution, ceSubstitution, useSiteSession, allowIdenticalSubstitution)
}

fun substitutorByMap(
    substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
    useSiteSession: FirSession,
    allowIdenticalSubstitution: Boolean = false,
): ConeSubstitutor {
    val valueSubstitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
    val ceSubstitution = mutableMapOf<FirTypeParameterSymbol, CEType>()
    for ((typeParameter, type) in substitution) {
        with (useSiteSession.typeContext) {
            val vt = type.projectOnValue() as ConeKotlinType
            valueSubstitution[typeParameter] = vt
            val cet = type.projectOnError()
            if (cet is ConeErrorUnionType) {
                ceSubstitution[typeParameter] = cet.errorType
            } else {
                ceSubstitution[typeParameter] = CEBotType // TODO: RE: suspicious place
            }
        }
    }
    return substitutorByMap(valueSubstitution, ceSubstitution, useSiteSession, allowIdenticalSubstitution)
}

class ConeSubstitutorByMap private constructor(
    // Used only for sake of optimizations at org.jetbrains.kotlin.analysis.api.fir.types.KtFirMapBackedSubstitutor
    val substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
    val ceSubstitution: Map<FirTypeParameterSymbol, CEType>,
    private val useSiteSession: FirSession
) : AbstractConeSubstitutor(useSiteSession.typeContext) {
    companion object {
        /**
         * See KDoc to [substitutorByMap]
         */
        fun create(
            substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
            ceSubstitution: Map<FirTypeParameterSymbol, CEType>,
            useSiteSession: FirSession,
            allowIdenticalSubstitution: Boolean,
        ): ConeSubstitutor {
            if (substitution.isEmpty() && ceSubstitution.isEmpty()) return Empty

            if (!allowIdenticalSubstitution) {
                // If all arguments match parameters, then substitutor isn't needed
                val substitutionIsIdentical = substitution.all { (parameterSymbol, argumentType) ->
                    (argumentType as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol == parameterSymbol && !argumentType.isMarkedNullable
                }
                val ceSubstitutionIsIdentical = ceSubstitution.all { (parameterSymbol, argumentType) ->
                    (argumentType as? CETypeParameterType)?.lookupTag?.typeParameterSymbol == parameterSymbol
                }
                if (substitutionIsIdentical && ceSubstitutionIsIdentical) {
                    return Empty
                }
            }
            return ConeSubstitutorByMap(substitution, ceSubstitution, useSiteSession)
        }
    }

    private val hashCode: Int by lazy(LazyThreadSafetyMode.PUBLICATION) {
        substitution.hashCode() + ceSubstitution.hashCode() * 31
    }

    override fun substituteCEType(type: CEType): CEType {
        if (type !is CETypeParameterType) return type
        return ceSubstitution[type.lookupTag.symbol] ?: return type
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
        if (ceSubstitution != other.ceSubstitution) return false
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

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds

class ConeRawScopeSubstitutor(private val useSiteSession: FirSession) : AbstractConeSubstitutor(useSiteSession.typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        return when {
            type is ConeTypeParameterType -> {
                substituteOrSelf(
                    type.lookupTag.symbol.getProjectionForRawType(useSiteSession, makeNullable = type.isMarkedNullable)
                )
            }

            type is ConeClassLikeType && type.typeArguments.isNotEmpty() -> {
                if (type.lookupTag.classId == StandardClassIds.Array) {
                    val argument = type.typeArguments[0]
                    val erasedType = argument.type?.let(this::substituteOrSelf)

                    return type.withArguments(
                        arrayOf(erasedType?.toTypeProjection(argument.kind) ?: ConeStarProjection)
                    )
                }

                val firClass = type.fullyExpandedType(useSiteSession).lookupTag.toRegularClassSymbol(useSiteSession) ?: return null
                val nullabilities = BooleanArray(type.typeArguments.size) { type.typeArguments[it].type?.isMarkedNullable == true }
                ConeRawType.create(
                    type.withArguments(
                        firClass.typeParameterSymbols.getProjectionsForRawType(useSiteSession, nullabilities = nullabilities)
                    ),
                    type.replaceArgumentsWithStarProjections()
                )
            }

            type is ConeFlexibleType -> {
                val substitutedLowerBound = substituteOrNull(type.lowerBound)
                val substitutedUpperBound = substituteOrNull(type.upperBound)
                if (substitutedLowerBound == null && substitutedUpperBound == null) return null

                val newLowerBound = substitutedLowerBound?.lowerBoundIfFlexible() ?: type.lowerBound
                val newUpperBound = substitutedUpperBound?.upperBoundIfFlexible() ?: type.upperBound

                if (substitutedLowerBound is ConeRawType || substitutedUpperBound is ConeRawType) {
                    return ConeRawType.create(newLowerBound, newUpperBound)
                }

                ConeFlexibleType(newLowerBound, newUpperBound)
            }

            else -> null
        }
    }

    override fun equals(other: Any?): Boolean = other is ConeRawScopeSubstitutor && useSiteSession == other.useSiteSession

    override fun hashCode(): Int = 0
}

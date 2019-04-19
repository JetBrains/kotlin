/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker


interface ConeSubstitutor : TypeSubstitutorMarker {
    fun substituteOrSelf(type: ConeKotlinType): ConeKotlinType
    fun substituteOrNull(type: ConeKotlinType): ConeKotlinType?

    object Empty : ConeSubstitutor {
        override fun substituteOrSelf(type: ConeKotlinType): ConeKotlinType {
            return type
        }

        override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
            return null
        }

    }
}

fun ConeSubstitutor.substituteOrNull(type: ConeKotlinType?): ConeKotlinType? {
    return type?.let { substituteOrNull(it) }
}

abstract class AbstractConeSubstitutor : ConeSubstitutor {
    private fun wrapProjection(old: ConeKotlinTypeProjection, newType: ConeKotlinType): ConeKotlinTypeProjection {
        return when (old) {
            is ConeStarProjection -> old
            is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
            is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
            is ConeKotlinType -> newType
            else -> old
        }
    }

    abstract fun substituteType(type: ConeKotlinType): ConeKotlinType?

    fun makeNullableIfNeed(isNullable: Boolean, type: ConeKotlinType?): ConeKotlinType? {
        if (!isNullable) return type
        return type?.withNullability(ConeNullability.NULLABLE)
    }

    override fun substituteOrSelf(type: ConeKotlinType): ConeKotlinType {
        return substituteOrNull(type) ?: type
    }

    override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        val newType = substituteType(type)
        return (newType ?: type.substituteRecursive()) ?: newType
    }

    private fun ConeKotlinType.substituteRecursive(): ConeKotlinType? {
        return when (this) {
            is ConeClassErrorType -> return null
            is ConeClassType -> this.substituteArguments()
            is ConeAbbreviatedType -> this.substituteArguments()
            is ConeTypeParameterType -> return null
            is ConeTypeVariableType -> return null
            is ConeFlexibleType -> this.substituteBounds()
            is ConeCapturedType -> return null
            is ConeDefinitelyNotNullType -> this.substituteOriginal()
        }
    }

    private fun ConeDefinitelyNotNullType.substituteOriginal(): ConeDefinitelyNotNullType? {
        TODO()
    }

    private fun ConeFlexibleType.substituteBounds(): ConeFlexibleType? {
        val newLowerBound = substituteOrNull(lowerBound)
        val newUpperBound = substituteOrNull(upperBound)
        if (newLowerBound != null || newUpperBound != null) {
            return ConeFlexibleType(
                newLowerBound?.lowerBoundIfFlexible() ?: lowerBound,
                newUpperBound?.upperBoundIfFlexible() ?: upperBound
            )
        }
        return null
    }

    private fun ConeKotlinType.substituteArguments(): ConeKotlinType? {
        val newArguments by lazy { arrayOfNulls<ConeKotlinTypeProjection>(typeArguments.size) }
        var initialized = false
        for ((index, typeArgument) in this.typeArguments.withIndex()) {
            val type = (typeArgument as? ConeTypedProjection)?.type ?: continue
            val newType = substituteOrNull(type)
            if (newType != null) {
                initialized = true
                newArguments[index] = wrapProjection(typeArgument, newType)
            }
        }

        if (initialized) {
            for ((index, typeArgument) in this.typeArguments.withIndex()) {
                if (newArguments[index] == null) {
                    newArguments[index] = typeArgument
                }
            }
            @Suppress("UNCHECKED_CAST")
            return when (this) {
                is ConeClassTypeImpl -> ConeClassTypeImpl(
                    lookupTag,
                    newArguments as Array<ConeKotlinTypeProjection>,
                    nullability.isNullable
                )
                is ConeAbbreviatedTypeImpl -> ConeAbbreviatedTypeImpl(
                    abbreviationLookupTag,
                    newArguments as Array<ConeKotlinTypeProjection>,
                    nullability.isNullable
                )
                is ConeClassLikeType -> error("Unknown class-like type to substitute: $this, ${this::class}")
                else -> error("Unknown type to substitute: $this, ${this::class}")
            }
        }
        return null
    }


}


class ConeSubstitutorByMap(val substitution: Map<ConeTypeParameterSymbol, ConeKotlinType>) : AbstractConeSubstitutor() {

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeTypeParameterType) return null
        return makeNullableIfNeed(type.isMarkedNullable, substitution[type.lookupTag])
    }
}

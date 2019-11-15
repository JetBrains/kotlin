/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker


abstract class ConeSubstitutor : TypeSubstitutorMarker {
    abstract fun substituteOrSelf(type: ConeKotlinType): ConeKotlinType
    abstract fun substituteOrNull(type: ConeKotlinType): ConeKotlinType?

    object Empty : ConeSubstitutor() {
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

abstract class AbstractConeSubstitutor : ConeSubstitutor() {
    protected fun wrapProjection(old: ConeKotlinTypeProjection, newType: ConeKotlinType): ConeKotlinTypeProjection {
        return when (old) {
            is ConeStarProjection -> old
            is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
            is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
            is ConeKotlinType -> newType
            else -> old
        }
    }

    abstract fun substituteType(type: ConeKotlinType): ConeKotlinType?
    open fun substituteArgument(projection: ConeKotlinTypeProjection): ConeKotlinTypeProjection? {
        val type = (projection as? ConeTypedProjection)?.type ?: return null
        val newType = substituteOrNull(type) ?: return null
        return wrapProjection(projection, newType)
    }

    fun makeNullableIfNeed(isNullable: Boolean, type: ConeKotlinType?): ConeKotlinType? {
        if (!isNullable) return type
        return type?.withNullability(ConeNullability.NULLABLE)
    }

    override fun substituteOrSelf(type: ConeKotlinType): ConeKotlinType {
        return substituteOrNull(type) ?: type
    }

    override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        val newType = substituteType(type)
        return (newType ?: type.substituteRecursive())
    }

    private fun ConeKotlinType.substituteRecursive(): ConeKotlinType? {
        return when (this) {
            is ConeClassErrorType -> return null
            is ConeClassType -> this.substituteArguments()
            is ConeAbbreviatedType -> this.substituteArguments()
            is ConeLookupTagBasedType -> return null
            is ConeFlexibleType -> this.substituteBounds()
            is ConeCapturedType -> return null
            is ConeDefinitelyNotNullType -> this.substituteOriginal()
            is ConeIntersectionType -> this.substituteIntersectedTypes()
            is ConeStubType -> return null
        }
    }

    private fun ConeIntersectionType.substituteIntersectedTypes(): ConeIntersectionType? {
        val substitutedTypes = ArrayList<ConeKotlinType>(intersectedTypes.size)
        var somethingIsSubstituted = false
        for (type in intersectedTypes) {
            val substitutedType = substituteOrNull(type)?.also {
                somethingIsSubstituted = true
            } ?: type
            substitutedTypes += substitutedType
        }
        if (!somethingIsSubstituted) return null
        return ConeIntersectionType(substitutedTypes)
    }

    private fun ConeDefinitelyNotNullType.substituteOriginal(): ConeDefinitelyNotNullType? {
        return ConeDefinitelyNotNullType.create(substituteType(original)?.withNullability(ConeNullability.NOT_NULL) ?: original)
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
            newArguments[index] = substituteArgument(typeArgument)?.also {
                initialized = true
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
                is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(
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

fun substitutorByMap(substitution: Map<FirTypeParameterSymbol, ConeKotlinType>): ConeSubstitutor {
    if (substitution.isEmpty()) return ConeSubstitutor.Empty
    return ConeSubstitutorByMap(substitution)
}

class ConeSubstitutorByMap(val substitution: Map<FirTypeParameterSymbol, ConeKotlinType>) : AbstractConeSubstitutor() {

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeTypeParameterType) return null
        return makeNullableIfNeed(type.isMarkedNullable, substitution[type.lookupTag.symbol])
    }
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.typeConstructor

abstract class AbstractConeSubstitutor : ConeSubstitutor() {
    abstract val typeInferenceContext: ConeInferenceContext

    private fun wrapProjection(old: ConeTypeProjection, newType: ConeKotlinType): ConeTypeProjection {
        return when (old) {
            is ConeStarProjection -> old
            is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
            is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
            is ConeKotlinType -> newType
            else -> old
        }
    }

    abstract fun substituteType(type: ConeKotlinType): ConeKotlinType?
    open fun substituteArgument(projection: ConeTypeProjection): ConeTypeProjection? {
        val type = (projection as? ConeKotlinTypeProjection)?.type ?: return null
        val newType = substituteOrNull(type) ?: return null
        return wrapProjection(projection, newType)
    }

    fun ConeKotlinType?.updateNullabilityIfNeeded(originalType: ConeKotlinType): ConeKotlinType? {
        return when {
            originalType is ConeDefinitelyNotNullType -> this?.withNullability(ConeNullability.NOT_NULL, typeInferenceContext)
            originalType.isMarkedNullable -> this?.withNullability(ConeNullability.NULLABLE, typeInferenceContext)
            else -> this
        }
    }

    override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        val newType = substituteType(type)
        if (newType != null && type is ConeDefinitelyNotNullType) {
            return newType.makeConeTypeDefinitelyNotNullOrNotNull(typeInferenceContext)
        }
        return (newType ?: type.substituteRecursive())
    }

    private fun ConeKotlinType.substituteRecursive(): ConeKotlinType? {
        return when (this) {
            is ConeClassErrorType -> return null
            is ConeClassLikeType -> this.substituteArguments()
            is ConeLookupTagBasedType -> return null
            is ConeFlexibleType -> this.substituteBounds()?.let {
                // TODO: may be (?) it's worth adding regular type comparison via AbstractTypeChecker
                // However, the simplified check here should be enough for typical flexible types
                if (it.lowerBound == it.upperBound) it.lowerBound
                else it
            }
            is ConeCapturedType -> return null
            is ConeDefinitelyNotNullType -> this.substituteOriginal()
            is ConeIntersectionType -> this.substituteIntersectedTypes()
            is ConeStubType -> return null
            is ConeIntegerLiteralType -> return null
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

    private fun ConeDefinitelyNotNullType.substituteOriginal(): ConeKotlinType? {
        val substituted = substituteOrNull(original)?.withNullability(ConeNullability.NOT_NULL, typeInferenceContext) ?: return null
        return ConeDefinitelyNotNullType.create(substituted) ?: substituted
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
        val newArguments by lazy { arrayOfNulls<ConeTypeProjection>(typeArguments.size) }
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
                    newArguments as Array<ConeTypeProjection>,
                    nullability.isNullable,
                    attributes
                )
                is ConeClassLikeType -> error("Unknown class-like type to substitute: $this, ${this::class}")
                else -> error("Unknown type to substitute: $this, ${this::class}")
            }
        }
        return null
    }


}

fun substitutorByMap(substitution: Map<FirTypeParameterSymbol, ConeKotlinType>, useSiteSession: FirSession): ConeSubstitutor {
    // If all arguments match parameters, then substitutor isn't needed
    if (substitution.all { (parameterSymbol, argumentType) ->
            (argumentType as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol == parameterSymbol
        }
    ) return ConeSubstitutor.Empty
    return ConeSubstitutorByMap(substitution, useSiteSession)
}

data class ChainedSubstitutor(private val first: ConeSubstitutor, private val second: ConeSubstitutor) : ConeSubstitutor() {
    override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        first.substituteOrNull(type)?.let { return second.substituteOrSelf(it) }
        return second.substituteOrNull(type)
    }
}

fun ConeSubstitutor.chain(other: ConeSubstitutor): ConeSubstitutor {
    if (this == ConeSubstitutor.Empty) return other
    if (other == ConeSubstitutor.Empty) return this
    return ChainedSubstitutor(this, other)
}

data class ConeSubstitutorByMap(
    val substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
    val useSiteSession: FirSession
) : AbstractConeSubstitutor() {
    override val typeInferenceContext: ConeInferenceContext
        get() = useSiteSession.inferenceComponents.ctx

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeTypeParameterType) return null
        val result = substitution[type.lookupTag.symbol].updateNullabilityIfNeeded(type) ?: return null
        if (type.isUnsafeVarianceType(useSiteSession)) {
            return useSiteSession.inferenceComponents.approximator.approximateToSuperType(
                result, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
            ) ?: result
        }
        return result
    }
}

fun createTypeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, ConeKotlinType>, context: ConeTypeContext): ConeSubstitutor {
    if (map.isEmpty()) return ConeSubstitutor.Empty
    return object : AbstractConeSubstitutor(), TypeSubstitutorMarker {
        override val typeInferenceContext: ConeInferenceContext
            get() = context.session.inferenceComponents.ctx

        override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
            if (type !is ConeLookupTagBasedType && type !is ConeStubType) return null
            val new = map[type.typeConstructor(context)] ?: return null
            return new.approximateIntegerLiteralType().updateNullabilityIfNeeded(type)
        }
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.withCombinedAttributesFrom
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.model.typeConstructor

abstract class AbstractConeSubstitutor(protected val typeContext: ConeTypeContext) : ConeSubstitutor() {
    protected fun wrapProjection(old: ConeTypeProjection, newType: ConeKotlinType): ConeTypeProjection {
        return when (old) {
            is ConeStarProjection -> old
            is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
            is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
            is ConeKotlinTypeConflictingProjection -> ConeKotlinTypeConflictingProjection(newType)
            is ConeKotlinType -> newType
            else -> old
        }
    }

    abstract fun substituteType(type: ConeKotlinType): ConeKotlinType?
    open fun substituteArgument(projection: ConeTypeProjection, lookupTag: ConeClassLikeLookupTag, index: Int): ConeTypeProjection? {
        val type = (projection as? ConeKotlinTypeProjection)?.type ?: return null
        val newType = substituteOrNull(type) ?: return null
        return wrapProjection(projection, newType)
    }

    fun ConeKotlinType?.updateNullabilityIfNeeded(originalType: ConeKotlinType): ConeKotlinType? {
        return when {
            originalType is ConeDefinitelyNotNullType -> this?.withNullability(ConeNullability.NOT_NULL, typeContext)
            originalType.isMarkedNullable -> this?.withNullability(ConeNullability.NULLABLE, typeContext)
            else -> this
        }
    }

    override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        val newType = substituteType(type)
        if (newType != null && type is ConeDefinitelyNotNullType) {
            return newType.makeConeTypeDefinitelyNotNullOrNotNull(typeContext)
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
            is ConeCapturedType -> return substituteCapturedType()
            is ConeDefinitelyNotNullType -> this.substituteOriginal()
            is ConeIntersectionType -> this.substituteIntersectedTypes()
            is ConeStubType -> return null
            is ConeIntegerLiteralType -> return null
        }
    }

    private fun ConeCapturedType.substituteCapturedType(): ConeCapturedType? {
        val innerType = this.lowerType ?: this.constructor.projection.type
        val substitutedInnerType = substituteOrNull(innerType) ?: return null
        if (substitutedInnerType is ConeCapturedType) return substitutedInnerType
        val substitutedSuperTypes =
            this.constructor.supertypes?.map { substituteOrSelf(it) }

        return ConeCapturedType(
            captureStatus,
            constructor = ConeCapturedTypeConstructor(
                wrapProjection(constructor.projection, substitutedInnerType),
                substitutedSuperTypes,
                typeParameterMarker = constructor.typeParameterMarker
            ),
            lowerType = if (lowerType != null) substitutedInnerType else null
        )
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
        val substituted = substituteOrNull(original)
            ?.withNullability(ConeNullability.NOT_NULL, typeContext)
            ?.withAttributes(original.attributes)
            ?: return null
        return ConeDefinitelyNotNullType.create(substituted, typeContext) ?: substituted
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

        require(this is ConeClassLikeType) { "Unknown type to substitute: $this, ${this::class}" }

        for ((index, typeArgument) in this.typeArguments.withIndex()) {
            newArguments[index] = substituteArgument(typeArgument, lookupTag, index)?.also {
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
                else -> error("Unknown class-like type to substitute: $this, ${this::class}")
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

class ConeSubstitutorByMap(
    // Used only for sake of optimizations at org.jetbrains.kotlin.analysis.api.fir.types.KtFirMapBackedSubstitutor
    val substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
    private val useSiteSession: FirSession
) : AbstractConeSubstitutor(useSiteSession.typeContext) {

    private val hashCode by lazy(LazyThreadSafetyMode.PUBLICATION) {
        substitution.hashCode()
    }

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeTypeParameterType) return null
        val result =
            substitution[type.lookupTag.symbol].updateNullabilityIfNeeded(type)
                ?.withCombinedAttributesFrom(type)
                ?: return null
        if (type.isUnsafeVarianceType(useSiteSession)) {
            return useSiteSession.typeApproximator.approximateToSuperType(
                result, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
            ) ?: result
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeSubstitutorByMap) return false

        if (hashCode != other.hashCode) return false
        if (substitution != other.substitution) return false
        if (useSiteSession != other.useSiteSession) return false

        return true
    }

    override fun hashCode() = hashCode
}

fun createTypeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, ConeKotlinType>, context: ConeTypeContext): ConeSubstitutor {
    if (map.isEmpty()) return ConeSubstitutor.Empty
    return object : AbstractConeSubstitutor(context), TypeSubstitutorMarker {
        override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
            if (type !is ConeLookupTagBasedType && type !is ConeStubType) return null
            val new = map[type.typeConstructor(context)] ?: return null
            return new.approximateIntegerLiteralType().updateNullabilityIfNeeded(type)?.withCombinedAttributesFrom(type)
        }
    }
}

internal class TypeSubstitutorByTypeConstructor(
    private val map: Map<TypeConstructorMarker, ConeKotlinType>,
    context: ConeTypeContext
) : AbstractConeSubstitutor(context), TypeSubstitutorMarker {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeLookupTagBasedType && type !is ConeStubType) return null
        val new = map[type.typeConstructor(typeContext)] ?: return null
        return new.approximateIntegerLiteralType().updateNullabilityIfNeeded(type)?.withCombinedAttributesFrom(type)
    }
}

// Note: builder inference uses TypeSubstitutorByTypeConstructor for not fixed type substitution
class NotFixedTypeToVariableSubstitutorForDelegateInference(
    val bindings: Map<TypeVariableMarker, ConeKotlinType>,
    typeContext: ConeTypeContext
) : AbstractConeSubstitutor(typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeStubType) return null
        if (type.constructor.isTypeVariableInSubtyping) return null
        return bindings[type.constructor.variable].updateNullabilityIfNeeded(type)
    }
}


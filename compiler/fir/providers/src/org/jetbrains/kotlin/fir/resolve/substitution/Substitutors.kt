/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.withCombinedAttributesFrom
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

inline fun ConeCapturedType.substitute(f: (ConeKotlinType) -> ConeKotlinType?): ConeCapturedType? {
    val innerType = this.lowerType ?: this.constructor.projection.type
    // TODO(KT-64024): This early return looks suspicious.
    //  In fact, if the inner type wasn't substituted we will ignore potential substitution in
    //  super types
    val substitutedInnerType = innerType?.let(f) ?: return null
    if (substitutedInnerType is ConeCapturedType) return substitutedInnerType
    val substitutedSuperTypes =
        this.constructor.supertypes?.map { f(it) ?: it }

    // TODO(KT-64027): Creation of new captured types creates unexpected behavior by breaking substitution consistency.
    //  E.g:
    //  ```
    //   substitution = { A => B }
    //   substituteOrSelf(C<CapturedType(out A)_0>) -> C<CapturedType(out B)_1>
    //   substituteOrSelf(C<CapturedType(out A)_0>) -> C<CapturedType(out B)_2>
    //   C<CapturedType(out B)_1> <!:> C<CapturedType(out B)_2>
    //  ```

    return copy(
        constructor = ConeCapturedTypeConstructor(
            wrapProjection(constructor.projection, substitutedInnerType),
            substitutedSuperTypes,
            typeParameterMarker = constructor.typeParameterMarker
        ),
        lowerType = if (lowerType != null) substitutedInnerType else null,
    )
}

fun wrapProjection(old: ConeTypeProjection, newType: ConeKotlinType): ConeTypeProjection {
    return when (old) {
        is ConeStarProjection -> old
        is ConeKotlinTypeProjectionIn -> ConeKotlinTypeProjectionIn(newType)
        is ConeKotlinTypeProjectionOut -> ConeKotlinTypeProjectionOut(newType)
        is ConeKotlinTypeConflictingProjection -> ConeKotlinTypeConflictingProjection(newType)
        is ConeKotlinType -> newType
        else -> old
    }
}

abstract class AbstractConeSubstitutor(protected val typeContext: ConeTypeContext) : ConeSubstitutor() {
    abstract fun substituteType(type: ConeKotlinType): ConeKotlinType?
    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val type = (projection as? ConeKotlinTypeProjection)?.type ?: return null
        val newType = substituteOrNull(type) ?: return null
        return wrapProjection(projection, newType)
    }

    companion object {
        fun ConeKotlinType.updateNullabilityIfNeeded(originalType: ConeKotlinType, typeContext: ConeTypeContext): ConeKotlinType {
            return when {
                originalType is ConeDefinitelyNotNullType -> this.withNullability(ConeNullability.NOT_NULL, typeContext)
                originalType.isMarkedNullable -> this.withNullability(ConeNullability.NULLABLE, typeContext)
                else -> this
            }
        }
    }

    fun ConeKotlinType.updateNullabilityIfNeeded(originalType: ConeKotlinType): ConeKotlinType =
        updateNullabilityIfNeeded(originalType, typeContext)

    override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        val newType = substituteType(type)

        if (newType != null && type is ConeDefinitelyNotNullType) {
            return newType.makeConeTypeDefinitelyNotNullOrNotNull(typeContext, avoidComprehensiveCheck = false)
        }

        val substitutedType = newType ?: type.substituteRecursive()

        // Don't substitute attributes of flexible types because their bounds will be processed individually.
        val substitutedAttributesOfUnsubstitutedType = runIf(type !is ConeFlexibleType) {
            type.attributes.transformTypesWith(this::substituteOrNull)
        }
        val substitutedAttributes = substitutedAttributesOfUnsubstitutedType?.let {
            // ConeAttribute.add is typically implemented to favor the RHS if both are not-null, so we use the substituted attributes as RHS.
            // We can't do `(substitutedType ?: type).attributes.transformTypesWith(this::substituteOrNull)` because it could lead to an
            // infinite loop in a situation like `{E -> Attr(E) Foo}` applied to `E`.
            substitutedType?.attributes?.add(it) ?: it
        }

        return if (substitutedType != null || substitutedAttributes != null) {
            var result = substitutedType ?: type

            if (substitutedAttributes != null) {
                result = result.withAttributes(substitutedAttributes)
            }

            result
        } else {
            null
        }
    }

    private fun ConeKotlinType.substituteRecursive(): ConeKotlinType? {
        return when (this) {
            is ConeClassLikeType -> this.substituteArguments()
            is ConeLookupTagBasedType, is ConeTypeVariableType -> return null
            is ConeFlexibleType -> this.substituteBounds()?.let {
                // TODO: may be (?) it's worth adding regular type comparison via AbstractTypeChecker
                // However, the simplified check here should be enough for typical flexible types
                if (it.lowerBound == it.upperBound) it.lowerBound
                else it
            }
            is ConeCapturedType -> return substitute(::substituteOrNull)
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
        val substitutedOriginal = substituteOrNull(original) ?: return null
        val substituted = substitutedOriginal.withNullability(
            ConeNullability.NOT_NULL,
            typeContext,
            substitutedOriginal.attributes.add(original.attributes),
            preserveAttributes = true,
        )
        return ConeDefinitelyNotNullType.create(
            substituted, typeContext, avoidComprehensiveCheck = true,
        ) ?: substituted
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

    private fun ConeSimpleKotlinType.substituteArguments(): ConeKotlinType? {
        val newArguments by lazy { arrayOfNulls<ConeTypeProjection>(typeArguments.size) }
        var initialized = false

        for ((index, typeArgument) in this.typeArguments.withIndex()) {
            newArguments[index] = substituteArgument(typeArgument, index)?.also {
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
                is ConeErrorType -> ConeErrorType(
                    diagnostic,
                    isUninferredParameter,
                    typeArguments = newArguments as Array<ConeTypeProjection>,
                    attributes = attributes
                )
                else -> errorWithAttachment("Unknown class-like type to substitute, ${this::class}") {
                    withConeTypeEntry("type", this@substituteArguments)
                }
            }
        }
        return null
    }


}

fun substitutorByMap(substitution: Map<FirTypeParameterSymbol, ConeKotlinType>, useSiteSession: FirSession): ConeSubstitutor {
    return ConeSubstitutorByMap.create(substitution, useSiteSession, allowIdenticalSubstitution = false)
}

data class ChainedSubstitutor(val first: ConeSubstitutor, val second: ConeSubstitutor) : ConeSubstitutor() {
    override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        first.substituteOrNull(type)?.let { return second.substituteOrSelf(it) }
        return second.substituteOrNull(type)
    }

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val firstResult = first.substituteArgument(projection, index)
        return second.substituteArgument(firstResult ?: projection, index) ?: firstResult
    }

    override fun toString(): String {
        return "$first then $second"
    }
}

fun ConeSubstitutor.chain(other: ConeSubstitutor): ConeSubstitutor {
    if (this == ConeSubstitutor.Empty) return other
    if (other == ConeSubstitutor.Empty) return this
    return ChainedSubstitutor(this, other)
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

class ConeRawScopeSubstitutor(
    private val useSiteSession: FirSession,
) : AbstractConeSubstitutor(useSiteSession.typeContext) {
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

                val firClass = type.fullyExpandedType(useSiteSession).lookupTag.toFirRegularClassSymbol(useSiteSession) ?: return null
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

    override fun equals(other: Any?): Boolean = other is ConeRawScopeSubstitutor

    override fun hashCode(): Int = 0
}

fun createTypeSubstitutorByTypeConstructor(
    map: Map<TypeConstructorMarker, ConeKotlinType>,
    context: ConeTypeContext,
    approximateIntegerLiterals: Boolean
): ConeSubstitutor {
    if (map.isEmpty()) return ConeSubstitutor.Empty
    return ConeTypeSubstitutorByTypeConstructor(map, context, approximateIntegerLiterals)
}

internal class ConeTypeSubstitutorByTypeConstructor(
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

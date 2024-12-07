/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.substitution

import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

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
                originalType is ConeDefinitelyNotNullType -> this.withNullability(nullable = false, typeContext)
                originalType.isMarkedNullable -> this.withNullability(nullable = true, typeContext)
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
            nullable = false,
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
                    isMarkedNullable,
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

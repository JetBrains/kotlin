/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.isNullableAny
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithNothing

class SamTypeApproximator(builtIns: KotlinBuiltIns, languageVersionSettings: LanguageVersionSettings) {
    private val typeApproximator = TypeApproximator(builtIns, languageVersionSettings)

    fun getSamTypeForValueParameter(
        valueParameter: ValueParameterDescriptor,
        carefulApproximationOfContravariantProjection: Boolean,
    ): KotlinType? {
        val singleArgumentType: KotlinType
        val originalSingleArgumentType: KotlinType?
        val varargElementType = valueParameter.varargElementType
        if (varargElementType != null) {
            singleArgumentType = varargElementType
            originalSingleArgumentType = valueParameter.original.varargElementType
            assert(originalSingleArgumentType != null) {
                "Value parameter and original value parameter have inconsistent varargs: " +
                        valueParameter + "; " + valueParameter.original
            }
        } else {
            singleArgumentType = valueParameter.type
            originalSingleArgumentType = valueParameter.original.type
        }
        if (singleArgumentType.isError || originalSingleArgumentType!!.isError) {
            return null
        }

        // This can be true in case when the value parameter is in the method of a generic type with out-projection.
        // We approximate Inv<Captured#1> to Nothing, while Inv itself can be a SAM interface safe to call here
        // (see testData genericSamProjectedOut.kt for details)
        // In such a case we can't have a proper supertype since wildcards are not allowed there,
        // so we use Nothing arguments instead that leads to a raw type used for a SAM wrapper.
        // See org.jetbrains.kotlin.codegen.state.KotlinTypeMapper#writeGenericType to understand how
        // raw types and Nothing arguments relate.
        val originalTypeToUse =
            if (KotlinBuiltIns.isNothing(singleArgumentType))
                originalSingleArgumentType.replaceArgumentsWithNothing()
            else
                singleArgumentType
        val approximatedOriginalTypeToUse =
            typeApproximator.approximateToSubType(
                originalTypeToUse,
                TypeApproximatorConfiguration.UpperBoundAwareIntersectionTypeApproximator
            ) ?: originalTypeToUse
        approximatedOriginalTypeToUse as KotlinType

        return approximatedOriginalTypeToUse.removeExternalProjections(carefulApproximationOfContravariantProjection)
    }

    // When changing this, please consider also changing the mirroring K2 function at
    // org.jetbrains.kotlin.fir.backend.generators.AdapterGenerator.removeExternalProjections
    private fun KotlinType.removeExternalProjections(carefulApproximationOfContravariantProjection: Boolean): KotlinType? {
        val newArguments = arguments.mapIndexed { i, argument ->
            if (carefulApproximationOfContravariantProjection && argument.projectionKind == Variance.IN_VARIANCE) {
                // Just erasing `in` from the type projection would lead to an incorrect type for the SAM adapter,
                // and error at runtime on JVM if invokedynamic + LambdaMetafactory is used, see KT-51868.
                // So we do it "carefully". If we have a class `A<T>` and a method that takes e.g. `A<in String>`, we check
                // if `T` has a non-trivial upper bound. If it has one, we don't attempt to perform a SAM conversion at all.
                // Otherwise we erase the type to `Any?`, so `A<in String>` becomes `A<Any?>`, which is the computed SAM type.
                val parameter = constructor.parameters.getOrNull(i) ?: return null
                val upperBound = parameter.upperBounds.singleOrNull()?.upperIfFlexible() ?: return null
                if (!upperBound.isNullableAny()) return null

                upperBound.asTypeProjection()
            } else TypeProjectionImpl(Variance.INVARIANT, argument.type)
        }
        return replace(newArguments)
    }
}

open class SamTypeFactory {
    open fun isSamType(type: KotlinType): Boolean {
        val descriptor = type.constructor.declarationDescriptor
        return descriptor is ClassDescriptor && descriptor.isFun
    }

    fun create(originalType: KotlinType): SamType? {
        return if (isSamType(originalType)) SamType(originalType) else null
    }
}

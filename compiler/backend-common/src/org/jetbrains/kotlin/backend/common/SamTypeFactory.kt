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
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithNothing

class SamTypeApproximator(builtIns: KotlinBuiltIns, languageVersionSettings: LanguageVersionSettings) {
    private val typeApproximator = TypeApproximator(builtIns, languageVersionSettings)

    fun getSamTypeForValueParameter(valueParameter: ValueParameterDescriptor): KotlinType? {
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

        return approximatedOriginalTypeToUse.removeExternalProjections()
    }

    private fun KotlinType.removeExternalProjections(): KotlinType {
        val newArguments = arguments.map { TypeProjectionImpl(Variance.INVARIANT, it.type) }
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
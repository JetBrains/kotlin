/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.types.KotlinType

open class GeneratorExtensions : StubGeneratorExtensions() {
    open val samConversion: SamConversion
        get() = SamConversion

    open class SamConversion {
        // Returns null if descriptor is not a SAM adapter
        open fun getOriginalForSamAdapter(descriptor: CallableDescriptor): CallableDescriptor? = null

        open fun isSamConstructor(descriptor: CallableDescriptor): Boolean = false

        open fun isSamType(type: KotlinType): Boolean = false

        open fun getSamTypeInfoForValueParameter(valueParameter: ValueParameterDescriptor): KotlinType? =
            throw UnsupportedOperationException("SAM conversion is not supported in this configuration (valueParameter=$valueParameter)")

        open fun getSubstitutedFunctionTypeForSamType(samType: KotlinType): KotlinType =
            throw UnsupportedOperationException("SAM conversion is not supported in this configuration (samType=$samType)")

        companion object Instance : SamConversion()
    }

    open fun computeFieldVisibility(descriptor: PropertyDescriptor): Visibility? = null

    open val enhancedNullability: EnhancedNullability
        get() = EnhancedNullability

    open class EnhancedNullability {
        open fun hasEnhancedNullability(kotlinType: KotlinType): Boolean = false

        open fun stripEnhancedNullability(kotlinType: KotlinType): KotlinType = kotlinType

        companion object Instance : EnhancedNullability()
    }

}

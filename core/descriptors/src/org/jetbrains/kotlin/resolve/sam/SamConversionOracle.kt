/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.sam

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.KotlinType

@DefaultImplementation(impl = SamConversionOracleDefault::class)
interface SamConversionOracle {
    fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean
    fun isPossibleSamType(samType: KotlinType): Boolean
    fun isJavaApplicableCandidate(candidate: CallableDescriptor): Boolean
}

class SamConversionOracleDefault : SamConversionOracle {
    override fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean = true

    override fun isPossibleSamType(samType: KotlinType): Boolean {
        val descriptor = samType.constructor.declarationDescriptor
        return descriptor is ClassDescriptor && descriptor.isFun
    }

    override fun isJavaApplicableCandidate(candidate: CallableDescriptor): Boolean = false
}
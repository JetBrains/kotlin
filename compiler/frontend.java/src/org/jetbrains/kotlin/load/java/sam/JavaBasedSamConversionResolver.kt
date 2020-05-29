/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.resolve.sam.SamConversionOracle
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver
import org.jetbrains.kotlin.synthetic.hasJavaOriginInHierarchy
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType

object JavaBasedSamConversionResolver : SamConversionResolver {
    override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? {
        if (classDescriptor !is JavaClassDescriptor) return null
        return classDescriptor.defaultFunctionTypeForSamInterface
    }
}

object JavaBasedSamConversionOracle : SamConversionOracle {
    override fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean {
        val functionDescriptor = candidate.original as? FunctionDescriptor ?: return false
        if (functionDescriptor is TypeAliasConstructorDescriptor &&
            functionDescriptor.underlyingConstructorDescriptor is JavaClassConstructorDescriptor
        ) return true

        return functionDescriptor.hasJavaOriginInHierarchy()
    }

    override fun isPossibleSamType(samType: KotlinType): Boolean {
        val descriptor = samType.constructor.declarationDescriptor
        return descriptor is ClassDescriptor && (descriptor.isFun || descriptor is JavaClassDescriptor)
    }

    override fun isJavaApplicableCandidate(candidate: CallableDescriptor): Boolean =
        shouldRunSamConversionForFunction(candidate)
}
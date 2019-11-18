/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.resolve.SamConversionResolver
import org.jetbrains.kotlin.types.SimpleType

object JavaBasedSamConversionResolver : SamConversionResolver {
    override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? {
        if (classDescriptor !is JavaClassDescriptor) return null
        return classDescriptor.defaultFunctionTypeForSamInterface
    }
}
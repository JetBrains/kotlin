/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.container.PlatformSpecificExtension
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.SimpleType

@DefaultImplementation(impl = SamConversionResolver.Empty::class)
interface SamConversionResolver : PlatformSpecificExtension<SamConversionResolver> {
    object Empty : SamConversionResolver {
        override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? = null
    }

    fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType?
}

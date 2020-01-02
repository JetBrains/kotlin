/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.CallableDescriptor

@DefaultImplementation(impl = PlatformOverloadsSpecificityComparator.None::class)
interface PlatformOverloadsSpecificityComparator {
    fun isMoreSpecificShape(specific: CallableDescriptor, general: CallableDescriptor): Boolean

    object None : PlatformOverloadsSpecificityComparator {
        override fun isMoreSpecificShape(specific: CallableDescriptor, general: CallableDescriptor) = false
    }
}

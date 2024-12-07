/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

abstract class DescriptorBasedDeprecationInfo : DeprecationInfo() {
    override val propagatesToOverrides: Boolean
        get() = forcePropagationToOverrides

    /**
     * Marks deprecation as necessary to propagate to overrides
     * even if LanguageFeature.StopPropagatingDeprecationThroughOverrides is disabled or one of the overrides "undeprecated"
     * See DeprecationResolver.deprecationByOverridden for details.
     *
     * Currently, it's only expected to be true for deprecation from unsupported JDK members that might be removed in future versions:
     * we'd like to mark their overrides as unsafe as well.
     *
     * Also, there's an implicit contract that if `forcePropagationToOverrides`, then `propagatesToOverrides` should also be true
     */
    open val forcePropagationToOverrides: Boolean
        get() = false

    abstract val target: DeclarationDescriptor
}

val DEPRECATED_FUNCTION_KEY = object : CallableDescriptor.UserDataKey<DescriptorBasedDeprecationInfo> {}

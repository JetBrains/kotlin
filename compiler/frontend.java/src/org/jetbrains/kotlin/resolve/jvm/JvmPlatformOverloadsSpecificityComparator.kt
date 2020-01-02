/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.calls.results.PlatformOverloadsSpecificityComparator

class JvmPlatformOverloadsSpecificityComparator(
    val languageVersionSettings: LanguageVersionSettings
) : PlatformOverloadsSpecificityComparator {
    override fun isMoreSpecificShape(specific: CallableDescriptor, general: CallableDescriptor): Boolean {
        if (specific !is PropertyDescriptor || general !is PropertyDescriptor) return false

        if (specific.dispatchReceiverParameter == null || general.dispatchReceiverParameter == null) return false
        if (specific.containingDeclaration !is ClassDescriptor) return false
        if (!DescriptorEquivalenceForOverrides
                .areEquivalent(specific.containingDeclaration, general.containingDeclaration, allowCopiesFromTheSameDeclaration = true)
        ) return false

        if (!languageVersionSettings.supportsFeature(LanguageFeature.PreferJavaFieldOverload)) return false

        return specific.isJavaField && !general.isJavaField
    }
}

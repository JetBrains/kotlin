/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

data class DescriptorWithDeprecation<out T : DeclarationDescriptor>(val descriptor: T, val isDeprecated: Boolean) {
    companion object {
        fun <T : DeclarationDescriptor> createNonDeprecated(descriptor: T): DescriptorWithDeprecation<T> =
            DescriptorWithDeprecation(descriptor, false)

        fun <T : DeclarationDescriptor> createDeprecated(descriptor: T): DescriptorWithDeprecation<T> =
            DescriptorWithDeprecation(descriptor, true)
    }
}


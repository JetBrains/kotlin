/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor

enum class OwnerKind {
    PACKAGE,
    IMPLEMENTATION,
    DEFAULT_IMPLS,
    ERASED_INLINE_CLASS,
    PROPERTY_REFERENCE_SIGNATURE;

    companion object {
        fun getMemberOwnerKind(descriptor: DeclarationDescriptor): OwnerKind = when (descriptor) {
            is PackageFragmentDescriptor -> PACKAGE
            is ClassDescriptor -> IMPLEMENTATION
            else -> throw AssertionError("Unexpected declaration container: $descriptor")
        }
    }
}

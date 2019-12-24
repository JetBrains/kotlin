/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedTypeParameterDescriptor

val TypeParameterDescriptor.originalTypeParameter: TypeParameterDescriptor
    get() =
        if (this is WrappedTypeParameterDescriptor) {
            original
        } else {
            when (val container = containingDeclaration.original) {
                is ClassifierDescriptorWithTypeParameters ->
                    container.declaredTypeParameters[index]
                is CallableDescriptor ->
                    container.typeParameters[index]
                else ->
                    throw AssertionError("Unexpected type parameter container: $container")
            }
        }

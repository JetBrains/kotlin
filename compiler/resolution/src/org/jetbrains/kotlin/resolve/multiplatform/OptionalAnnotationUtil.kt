/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.descriptorUtil.module

object OptionalAnnotationUtil {
    val OPTIONAL_EXPECTATION_FQ_NAME = StandardClassIds.Annotations.OptionalExpectation.asSingleFqName()

    @JvmStatic
    fun shouldGenerateExpectClass(descriptor: ClassDescriptor): Boolean {
        assert(descriptor.isExpect) { "Not an expected class: $descriptor" }

        if (isOptionalAnnotationClass(descriptor)) {
            return descriptor.findCompatibleActualsForExpected(descriptor.module).isEmpty()
        }

        return false
    }

    @JvmStatic
    fun isOptionalAnnotationClass(descriptor: DeclarationDescriptor): Boolean =
        descriptor is ClassDescriptor &&
                descriptor.kind == ClassKind.ANNOTATION_CLASS &&
                descriptor.isExpect &&
                descriptor.annotations.hasAnnotation(OPTIONAL_EXPECTATION_FQ_NAME)
}

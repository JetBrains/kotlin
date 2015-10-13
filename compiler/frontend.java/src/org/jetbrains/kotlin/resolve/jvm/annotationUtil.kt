/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.jvm.annotations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils

public fun DeclarationDescriptor.hasJvmOverloadsAnnotation(): Boolean {
    return getAnnotations().findAnnotation(FqName("kotlin.jvm.JvmOverloads")) != null
}

public fun DeclarationDescriptor.findJvmFieldAnnotation(): AnnotationDescriptor? {
    val fqName = FqName("kotlin.jvm.JvmField")
    val annotation = annotations.findAnnotation(fqName)
    if (annotation != null) {
        return annotation;
    }

    return annotations.getUseSiteTargetedAnnotations().asSequence().filter {
        it.target == AnnotationUseSiteTarget.FIELD
    }.map { it.annotation }.firstOrNull {
        val descriptor = it.getType().getConstructor().getDeclarationDescriptor()
        descriptor is ClassDescriptor && fqName.toUnsafe() == DescriptorUtils.getFqName(descriptor)
    }
}

public fun DeclarationDescriptor.hasJvmFieldAnnotation(): Boolean {
    return findJvmFieldAnnotation() != null
}
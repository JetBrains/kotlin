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

package org.jetbrains.kotlin.resolve.annotations

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ErrorValue

private val JVM_STATIC_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmStatic")

val JVM_FIELD_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmField")

fun DeclarationDescriptor.hasJvmStaticAnnotation(): Boolean {
    return annotations.findAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) != null
}

private val JVM_SYNTHETIC_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmSynthetic")

fun DeclarationDescriptor.hasJvmSyntheticAnnotation() = findJvmSyntheticAnnotation() != null

fun DeclarationDescriptor.findJvmSyntheticAnnotation() =
    DescriptorUtils.getAnnotationByFqName(annotations, JVM_SYNTHETIC_ANNOTATION_FQ_NAME)

private val STRICTFP_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Strictfp")

fun DeclarationDescriptor.findStrictfpAnnotation() =
    DescriptorUtils.getAnnotationByFqName(annotations, STRICTFP_ANNOTATION_FQ_NAME)

fun AnnotationDescriptor.argumentValue(parameterName: String): ConstantValue<*>? {
    return allValueArguments[Name.identifier(parameterName)].takeUnless { it is ErrorValue }
}

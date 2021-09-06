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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.ErrorValue

// This annotation is declared here in frontend (as opposed to frontend.java) because it's used in MainFunctionDetector.
// If you wish to add another JVM-related annotation and has/find utility methods, please proceed to jvmAnnotationUtil.kt
val JVM_STATIC_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmStatic")
val JVM_STATIC_ANNOTATION_CLASS_ID = ClassId.topLevel(JVM_STATIC_ANNOTATION_FQ_NAME)

fun DeclarationDescriptor.hasJvmStaticAnnotation(): Boolean {
    return annotations.findAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) != null
}

fun AnnotationDescriptor.argumentValue(parameterName: String): ConstantValue<*>? {
    return allValueArguments[Name.identifier(parameterName)].takeUnless { it is ErrorValue }
}

@Deprecated(
    "Use org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME or " +
            "org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation instead.",
    ReplaceWith("org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME"),
    DeprecationLevel.ERROR
)
val JVM_FIELD_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmField")


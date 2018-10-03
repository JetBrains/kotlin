/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.annotations

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils

val JVM_DEFAULT_FQ_NAME = FqName("kotlin.jvm.JvmDefault")
val JVM_OVERLOADS_FQ_NAME = FqName("kotlin.jvm.JvmOverloads")
val JVM_SYNTHETIC_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmSynthetic")

@JvmField
val SYNCHRONIZED_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Synchronized")

@JvmField
val STRICTFP_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Strictfp")

@JvmField
val VOLATILE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Volatile")

fun DeclarationDescriptor.findJvmOverloadsAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(JVM_OVERLOADS_FQ_NAME)

fun DeclarationDescriptor.findJvmFieldAnnotation(): AnnotationDescriptor? =
    (this as? PropertyDescriptor)?.backingField?.annotations?.findAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME)

fun DeclarationDescriptor.hasJvmFieldAnnotation(): Boolean =
    findJvmFieldAnnotation() != null

fun DeclarationDescriptor.isCallableMemberWithJvmDefaultAnnotation(): Boolean =
    this is CallableMemberDescriptor && hasJvmDefaultAnnotation()

fun CallableMemberDescriptor.hasJvmDefaultAnnotation(): Boolean =
    DescriptorUtils.getDirectMember(this).annotations.hasAnnotation(JVM_DEFAULT_FQ_NAME)

private fun Annotated.findJvmSyntheticAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)
        ?: (this as? PropertyDescriptor)?.backingField?.annotations?.findAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)

fun DeclarationDescriptor.hasJvmSyntheticAnnotation(): Boolean =
    findJvmSyntheticAnnotation() != null

fun DeclarationDescriptor.findStrictfpAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(STRICTFP_ANNOTATION_FQ_NAME)

fun DeclarationDescriptor.findSynchronizedAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(SYNCHRONIZED_ANNOTATION_FQ_NAME)

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.annotations

import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_RECORD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

fun DeclarationDescriptor.findJvmFieldAnnotation(): AnnotationDescriptor? =
    (this as? PropertyDescriptor)?.backingField?.annotations?.findAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME)

fun DeclarationDescriptor.hasJvmFieldAnnotation(): Boolean =
    findJvmFieldAnnotation() != null

fun CallableMemberDescriptor.isCompiledToJvmDefault(jvmDefault: JvmDefaultMode): Boolean {
    val directMember = DescriptorUtils.getDirectMember(this)

    val clazz = directMember.containingDeclaration

//  TODO add checks after fixes in diagnostics
//    assert(this.kind.isReal && isInterface(clazz) && modality != Modality.ABSTRACT) {
//        "`isCompiledToJvmDefault` should be called on non-fakeoverrides and non-abstract methods from interfaces $this"
//    }

    if (directMember.annotations.hasAnnotation(JVM_DEFAULT_FQ_NAME)) return true
    if (clazz !is DeserializedClassDescriptor) return jvmDefault.isEnabled
    return JvmProtoBufUtil.isNewPlaceForBodyGeneration(clazz.classProto)
}

fun CallableMemberDescriptor.hasObsoleteJvmDefaultAnnotation(): Boolean =
    DescriptorUtils.getDirectMember(this).annotations.hasAnnotation(JVM_DEFAULT_FQ_NAME)

private fun Annotated.findJvmSyntheticAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)
        ?: (this as? PropertyDescriptor)?.backingField?.annotations?.findAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)

fun DeclarationDescriptor.hasJvmSyntheticAnnotation(): Boolean =
    findJvmSyntheticAnnotation() != null

fun ClassDescriptor.isJvmRecord(): Boolean = annotations.hasAnnotation(JVM_RECORD_ANNOTATION_FQ_NAME)

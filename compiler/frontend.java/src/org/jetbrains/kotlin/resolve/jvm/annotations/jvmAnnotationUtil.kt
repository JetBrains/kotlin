/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.annotations

import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.util.findImplementationFromInterface
import org.jetbrains.kotlin.utils.ReportLevel

val JVM_DEFAULT_FQ_NAME = FqName("kotlin.jvm.JvmDefault")
val JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME = FqName("kotlin.jvm.JvmDefaultWithoutCompatibility")
val JVM_OVERLOADS_FQ_NAME = FqName("kotlin.jvm.JvmOverloads")

@JvmField
val JVM_SYNTHETIC_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmSynthetic")
val JVM_RECORD_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmRecord")

@JvmField
val SYNCHRONIZED_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Synchronized")

@JvmField
val STRICTFP_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Strictfp")

@JvmField
val VOLATILE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Volatile")

@JvmField
val TRANSIENT_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Transient")

fun DeclarationDescriptor.findJvmOverloadsAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(JVM_OVERLOADS_FQ_NAME)

fun DeclarationDescriptor.findJvmFieldAnnotation(): AnnotationDescriptor? =
    (this as? PropertyDescriptor)?.backingField?.annotations?.findAnnotation(JVM_FIELD_ANNOTATION_FQ_NAME)

fun DeclarationDescriptor.hasJvmFieldAnnotation(): Boolean =
    findJvmFieldAnnotation() != null

fun DeclarationDescriptor.isCallableMemberCompiledToJvmDefault(jvmDefault: JvmDefaultMode): Boolean =
    this is CallableMemberDescriptor && isCompiledToJvmDefault(jvmDefault)

fun CallableMemberDescriptor.isCompiledToJvmDefault(jvmDefault: JvmDefaultMode): Boolean {
    val directMember = DescriptorUtils.getDirectMember(this)

    val clazz = directMember.containingDeclaration

//  TODO add checks after fixes in diagnostics
//    assert(this.kind.isReal && isInterface(clazz) && modality != Modality.ABSTRACT) {
//        "`isCompiledToJvmDefault` should be called on non-fakeoverrides and non-abstract methods from interfaces $this"
//    }

    if (directMember.annotations.hasAnnotation(JVM_DEFAULT_FQ_NAME)) return true
    if (clazz !is DeserializedClassDescriptor) return jvmDefault.forAllMethodsWithBody
    return JvmProtoBufUtil.isNewPlaceForBodyGeneration(clazz.classProto)
}

fun CallableMemberDescriptor.checkIsImplementationCompiledToJvmDefault(jvmDefaultMode: JvmDefaultMode): Boolean {
    val actualImplementation =
        (if (kind.isReal) this else findImplementationFromInterface(this))
            ?: error("Can't find actual implementation for $this")
    return actualImplementation.isCallableMemberCompiledToJvmDefault(jvmDefaultMode)
}

fun CallableMemberDescriptor.hasJvmDefaultAnnotation(): Boolean =
    DescriptorUtils.getDirectMember(this).annotations.hasAnnotation(JVM_DEFAULT_FQ_NAME)

fun DeclarationDescriptor.hasJvmDefaultNoCompatibilityAnnotation(): Boolean =
    this.annotations.hasAnnotation(JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME)


fun CallableMemberDescriptor.hasPlatformDependentAnnotation(): Boolean =
    DescriptorUtils.getDirectMember(this).annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)

private fun Annotated.findJvmSyntheticAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)
        ?: (this as? PropertyDescriptor)?.backingField?.annotations?.findAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)

fun DeclarationDescriptor.hasJvmSyntheticAnnotation(): Boolean =
    findJvmSyntheticAnnotation() != null

fun DeclarationDescriptor.findStrictfpAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(STRICTFP_ANNOTATION_FQ_NAME)

fun DeclarationDescriptor.findSynchronizedAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(SYNCHRONIZED_ANNOTATION_FQ_NAME)

fun ClassDescriptor.isJvmRecord(): Boolean = annotations.hasAnnotation(JVM_RECORD_ANNOTATION_FQ_NAME)

data class NullabilityAnnotationsStatus(
    val reportLevelBefore: ReportLevel,
    val sinceVersion: LanguageVersion = LanguageVersion.KOTLIN_1_0,
    val reportLevelAfter: ReportLevel = reportLevelBefore,
) {
    companion object {
        val DEFAULT = NullabilityAnnotationsStatus(ReportLevel.STRICT)
    }
}

val nullabilityAnnotationSettings = mapOf(
    FqName("org.jetbrains.annotations") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("androidx.annotation") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("android.support.annotation") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("android.annotation") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("com.android.annotations") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("org.eclipse.jdt.annotation") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("org.checkerframework.checker.nullness.qual") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("org.checkerframework.checker.nullness.compatqual") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("javax.annotation") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("javax.annotation") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("edu.umd.cs.findbugs.annotations") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("io.reactivex.annotations") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("lombok") to NullabilityAnnotationsStatus.DEFAULT,
    FqName("org.jspecify.nullness") to NullabilityAnnotationsStatus(
        reportLevelBefore = ReportLevel.WARN,
        sinceVersion = LanguageVersion.KOTLIN_1_6,
        reportLevelAfter = ReportLevel.STRICT
    ),
)
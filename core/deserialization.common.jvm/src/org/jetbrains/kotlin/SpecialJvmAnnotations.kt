/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId

object SpecialJvmAnnotations {
    val SPECIAL_ANNOTATIONS: Set<ClassId> = listOf(
        JvmAnnotationNames.METADATA_FQ_NAME,
        JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION,
        JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION,
        JvmAnnotationNames.TARGET_ANNOTATION,
        JvmAnnotationNames.RETENTION_ANNOTATION,
        JvmAnnotationNames.DOCUMENTED_ANNOTATION
    ).mapTo(mutableSetOf(), ClassId::topLevel)

    val JAVA_LANG_ANNOTATION_REPEATABLE = ClassId.topLevel(JvmAnnotationNames.REPEATABLE_ANNOTATION)

    fun isAnnotatedWithContainerMetaAnnotation(klass: KotlinJvmBinaryClass): Boolean {
        var result = false
        klass.loadClassAnnotations(object : KotlinJvmBinaryClass.AnnotationVisitor {
            override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                if (classId == JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_META_ANNOTATION) {
                    result = true
                }
                return null
            }

            override fun visitEnd() {
            }
        }, null)
        return result
    }
}

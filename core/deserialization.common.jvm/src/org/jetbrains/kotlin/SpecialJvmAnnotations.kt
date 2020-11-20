/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object SpecialJvmAnnotations {
    val SPECIAL_ANNOTATIONS: Set<ClassId> = listOf(
        JvmAnnotationNames.METADATA_FQ_NAME,
        JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION,
        JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION,
        FqName("java.lang.annotation.Target"),
        FqName("java.lang.annotation.Retention"),
        FqName("java.lang.annotation.Documented")
    ).mapTo(mutableSetOf(), ClassId::topLevel)
}

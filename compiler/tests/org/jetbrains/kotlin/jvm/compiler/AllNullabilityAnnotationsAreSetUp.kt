/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.load.java.nullabilityAnnotationSettings
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase

class AllNullabilityAnnotationsAreSetUpTest : KtUsefulTestCase() {
    fun testAllAnnotationsAreSetUp() {
        assert(NULLABILITY_ANNOTATIONS.all { annotation -> nullabilityAnnotationSettings.keys.any { annotation.isChildOf(it) } }) {
            val missedAnnotations = NULLABILITY_ANNOTATIONS.filter { annotation ->
                nullabilityAnnotationSettings.keys.none { annotation.isChildOf(it) }
            }
            "Not all nullability annotations are presented in `nullabilityAnnotationSettings`. Missed annotations: $missedAnnotations"
        }
    }

    fun testAllSetUpAnnotationsArePresent() {
        assert(nullabilityAnnotationSettings.keys.all { annotationsPackage ->
            NULLABILITY_ANNOTATIONS.any { it.isChildOf(annotationsPackage) }
        }) {
            val missedAnnotations = nullabilityAnnotationSettings.keys.filter { annotationsPackage ->
                NULLABILITY_ANNOTATIONS.none { it.isChildOf(annotationsPackage) }
            }
            "Not all set up nullability annotations are presented in `NULLABILITY_ANNOTATIONS`. Missed annotations: $missedAnnotations"
        }
    }
}

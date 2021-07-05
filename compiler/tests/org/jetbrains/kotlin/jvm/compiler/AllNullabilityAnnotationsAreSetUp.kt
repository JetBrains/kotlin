/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATION_SETTINGS
import org.jetbrains.kotlin.load.java.NullabilityAnnotationStatesImpl
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase

class AllNullabilityAnnotationsAreSetUpTest : KtUsefulTestCase() {
    fun testAllAnnotationsAreSetUp() {
        val annotationsRawMap = (NULLABILITY_ANNOTATION_SETTINGS as NullabilityAnnotationStatesImpl).states
        assert(NULLABILITY_ANNOTATIONS.all { annotation -> annotationsRawMap.keys.any { annotation.isChildOf(it) } }) {
            val missedAnnotations = NULLABILITY_ANNOTATIONS.filter { annotation ->
                annotationsRawMap.keys.none { annotation == it || annotation.isChildOf(it) }
            }
            "Not all nullability annotations are presented in `nullabilityAnnotationSettings`. Missed annotations: $missedAnnotations"
        }
    }

    fun testAllSetUpAnnotationsArePresent() {
        val annotationsRawMap = (NULLABILITY_ANNOTATION_SETTINGS as NullabilityAnnotationStatesImpl).states
        assert(annotationsRawMap.keys.all { annotations ->
            NULLABILITY_ANNOTATIONS.any { it == annotations || it.isChildOf(annotations) }
        }) {
            val missedAnnotations = annotationsRawMap.keys.filter { annotationsPackage ->
                NULLABILITY_ANNOTATIONS.none { it.isChildOf(annotationsPackage) }
            }
            "Not all set up nullability annotations are presented in `NULLABILITY_ANNOTATIONS`. Missed annotations: $missedAnnotations"
        }
    }
}

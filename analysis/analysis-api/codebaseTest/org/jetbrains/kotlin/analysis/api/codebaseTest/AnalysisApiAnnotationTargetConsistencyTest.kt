/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationTarget
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.lang.reflect.Modifier

@OptIn(KaExperimentalApi::class)
class AnalysisApiAnnotationTargetConsistencyTest {
    @Test
    fun testStdlibConsistency() {
        val expectedFieldNames = AnnotationTarget.entries.mapTo(HashSet()) { it.name }

        val kaAnnotationTargetClass = KaAnnotationTarget::class.java
        val actualFieldNames = kaAnnotationTargetClass.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == kaAnnotationTargetClass }
            .mapTo(HashSet()) { it.name }

        // We cannot require equality as the standard library may be slightly outdated
        // (there might not be experimental annotation targets yet)
        val missingEntries = (expectedFieldNames - actualFieldNames).sorted()

        if (missingEntries.isNotEmpty()) {
            fail("Missing entries in ${KaAnnotationTarget::class.qualifiedName}: $missingEntries")
        }
    }
}

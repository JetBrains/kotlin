/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaJvmTarget
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(KaExperimentalApi::class)
class AnalysisApiJvmTargetConsistencyTest {
    private val jvmTargetClass = KaJvmTarget::class.java
    private val jvmTargetCompanionClass = KaJvmTarget.Companion::class.java

    @Test
    fun testJvmTargets() {
        @Suppress("UNCHECKED_CAST")
        val allTargetsExpected = jvmTargetClass.getField("ALL_TARGETS").get(null) as List<KaJvmTarget>

        val allTargetsActual = jvmTargetClass.fields
            .filter { it.name.startsWith("JVM_") }
            .map { it.get(null) as KaJvmTarget }
            .sortedBy { it.bytecodeMajorVersion }

        assertEquals(allTargetsExpected, allTargetsActual)
    }

    @Test
    fun testJvmFieldsNotForgotten() {
        val badMethods = (jvmTargetClass.declaredMethods + jvmTargetCompanionClass.declaredMethods)
            .filter { it.name.startsWith("getJVM_") }

        if (badMethods.isNotEmpty()) {
            val badPropertyNames = badMethods.map { it.name.removePrefix("get") }
            fail("@JvmField annotation is forgotten for properties $badPropertyNames")
        }
    }
}

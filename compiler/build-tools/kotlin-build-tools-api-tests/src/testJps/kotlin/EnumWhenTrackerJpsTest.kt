/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("The JPS enum-when tracker")
class EnumWhenTrackerJpsTest : BaseJpsTest() {

    @DisplayName("A `when` expression over a Java enum is reported")
    @TestMetadata("jps-enum-when")
    @Test
    fun whenOverJavaEnumIsReported() {
        val fixture = "jps-enum-when"
        jvmProject(inProcess) {
            val module = module(fixture)
            val enumWhenTracker = RecordingEnumWhenTracker()
            module.compile(compilationConfigAction = { builder ->
                builder.withJpsIc {
                    this[JvmJpsManagedIncrementalCompilationConfiguration.ENUM_WHEN_TRACKER] = enumWhenTracker
                }
            }) {
                assertTrue(enumWhenTracker.reports.isNotEmpty()) { "Enum-when tracker didn't produce any output" }
                assertEquals(
                    setOf("/src/com/example/usage.kt" to "com.example.Color"),
                    enumWhenTracker.reports.map { it.first.relativeToModule(fixture) to it.second }.toSet(),
                )
            }
        }
    }
}

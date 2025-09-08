/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Disabled("Example tests for evaluation purposes of the DSL")
class ExampleCompatibilityTest {
    @Test
    @DisplayName("Sample compatibility test that is run as part of each test suit")
    fun testDefaultNonIncrementalSettings() {
        val config = KotlinToolchain.loadImplementation(ExampleCompatibilityTest::class.java.classLoader).createDaemonExecutionPolicy()
        config[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS] = emptyList()
        assertEquals(emptyList<String>(), config[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS])
    }
}
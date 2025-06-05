/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@DisplayName("configureDaemonJVMOptions")
class ConfigureDaemonJvmOptionsTest {

    @Test
    fun `inheritMemoryLimits should produce default maxMemory`() {
        val opts = configureDaemonJVMOptions(
            DaemonJVMOptions(),
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )
        assertNotEquals("", opts.maxMemory)
    }

    @Test
    fun `inheritMemoryLimits should keep maxMemory`() {
        val opts = configureDaemonJVMOptions(
            DaemonJVMOptions(maxMemory = "maxMemoryValue"),
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )
        assertEquals("maxMemoryValue", opts.maxMemory)
    }
}

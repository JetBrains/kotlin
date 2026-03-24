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
        assertNotEquals("", opts.maxHeapSize)
        assertEquals("", opts.maxRam)
        assertEquals("", opts.maxRamFraction)
        assertEquals("", opts.maxRamPercentage)
    }

    @Test
    fun `inheritMemoryLimits should keep maxMemory`() {
        val opts = configureDaemonJVMOptions(
            DaemonJVMOptions(maxHeapSize = "maxMemoryValue"),
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )
        assertEquals("maxMemoryValue", opts.maxHeapSize)
        assertEquals("", opts.maxRam)
        assertEquals("", opts.maxRamFraction)
        assertEquals("", opts.maxRamPercentage)
    }

    @Test
    fun `inheritMemoryLimits should keep maxRam`() {
        val opts = configureDaemonJVMOptions(
            DaemonJVMOptions(maxRam = "maxRamValue"),
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )
        assertEquals("", opts.maxHeapSize)
        assertEquals("maxRamValue", opts.maxRam)
        assertEquals("", opts.maxRamFraction)
        assertEquals("", opts.maxRamPercentage)
    }

    @Test
    fun `inheritMemoryLimits should keep maxRamFraction`() {
        val opts = configureDaemonJVMOptions(
            DaemonJVMOptions(maxRamFraction = "maxRamFractionValue"),
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )
        assertEquals("", opts.maxHeapSize)
        assertEquals("", opts.maxRam)
        assertEquals("maxRamFractionValue", opts.maxRamFraction)
        assertEquals("", opts.maxRamPercentage)
    }

    @Test
    fun `inheritMemoryLimits should keep maxRamPercentage`() {
        val opts = configureDaemonJVMOptions(
            DaemonJVMOptions(maxRamPercentage = "maxRamPercentageValue"),
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )
        assertEquals("", opts.maxHeapSize)
        assertEquals("", opts.maxRam)
        assertEquals("", opts.maxRamFraction)
        assertEquals("maxRamPercentageValue", opts.maxRamPercentage)
    }

    @Test
    fun `inheritMemoryLimits should keep all limits`() {
        val opts = configureDaemonJVMOptions(
            DaemonJVMOptions(
                maxHeapSize = "maxMemoryValue",
                maxRam = "maxRamValue",
                maxRamFraction = "maxRamFractionValue",
                maxRamPercentage = "maxRamPercentageValue"
            ),
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )
        assertEquals("maxMemoryValue", opts.maxHeapSize)
        assertEquals("maxRamValue", opts.maxRam)
        assertEquals("maxRamFractionValue", opts.maxRamFraction)
        assertEquals("maxRamPercentageValue", opts.maxRamPercentage)
    }
}

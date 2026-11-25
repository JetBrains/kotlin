/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApplePlatformTest {

    @Test
    fun `generic destination uses the destination platform`() {
        assertEquals("generic/platform=iOS Simulator", ApplePlatform.IOS_SIMULATOR.genericDestination)
        assertEquals("generic/platform=macOS", ApplePlatform.MACOS.genericDestination)
    }

    @Test
    fun `every platform round-trips through its SDK name`() {
        ApplePlatform.entries.forEach { assertEquals(it, ApplePlatform.fromSdkName(it.sdkName)) }
        assertNull(ApplePlatform.fromSdkName("android"))
    }

    @Test
    fun `simulator platforms are the ones with a simulator SDK`() {
        assertEquals(
            ApplePlatform.entries.filter { it.sdkName.endsWith("simulator") }.toSet(),
            ApplePlatform.entries.filter { it.isSimulator }.toSet(),
        )
    }
}

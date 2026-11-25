/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EnvironmentCleanupTest {

    @Test
    fun `plus removes what either side removes`() {
        val combined = EnvironmentCleanup.SDKROOT + EnvironmentCleanup(names = setOf("A"), prefixes = setOf("B_"))

        assertEquals(EnvironmentCleanup(names = setOf("SDKROOT", "A"), prefixes = setOf("B_")), combined)
        assertEquals(EnvironmentCleanup.DEBUG_DYLIB, EnvironmentCleanup.DEBUG_DYLIB + EnvironmentCleanup.NONE)
    }

    @Test
    fun `removes matches exact names and prefixes`() {
        val cleanup = EnvironmentCleanup(names = setOf("SDKROOT"), prefixes = setOf("OTHER_"))

        assertEquals(
            listOf("SDKROOT", "OTHER_LDFLAGS", "OTHER_"),
            listOf("SDKROOT", "OTHER_LDFLAGS", "OTHER_", "SDKROOT_EXTRA", "MY_OTHER_FLAGS", "sdkroot").filter(cleanup::removes),
        )
    }

    @Test
    fun `presets match what KGP removes today`() {
        assertEquals(EnvironmentCleanup(), EnvironmentCleanup.NONE)
        assertEquals(EnvironmentCleanup(prefixes = setOf("SDK")), EnvironmentCleanup.SDK_VARIABLES)
        assertEquals(
            setOf("ENABLE_DEBUG_DYLIB", "EXECUTABLE_BLANK_INJECTION_DYLIB_PATH", "EXECUTABLE_DEBUG_DYLIB_INSTALL_NAME", "EXECUTABLE_DEBUG_DYLIB_PATH"),
            EnvironmentCleanup.DEBUG_DYLIB.names,
        )
        assertEquals(
            EnvironmentCleanup(
                names = EnvironmentCleanup.DEBUG_DYLIB.names + "EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES",
                prefixes = setOf("OTHER_", "ASSETCATALOG_"),
            ),
            EnvironmentCleanup.APP_BUILD_SETTINGS,
        )
    }
}

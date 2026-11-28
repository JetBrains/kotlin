/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.konan

import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Keeps the table of [NativePlatformToolchainImpl.supportedHosts] equal to `HostManager` for as long as `HostManager` exists.
 * Delete this test together with `HostManager`.
 */
class NativeSupportedHostsTest {
    @Test
    fun testSupportedHostsEqualHostManager() {
        val expected = HostManager().enabledByHost.entries.associate { [host, targets] -> host.name to targets.map { it.name }.toSet() }
        val actual = NativePlatformToolchainImpl("").supportedHosts.associate {
            it.target.stringValue to it.linkableTargets.map { target -> target.stringValue }.toSet()
        }
        assertEquals(expected, actual)
    }
}

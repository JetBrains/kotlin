/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KonanTarget
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain.Companion.native
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.testFederation.MustRunAlways
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@MustRunAlways
@DisplayName("Hosts supported by the Kotlin/Native compiler")
class NativeSupportedHostsTest {
    private val hosts = KotlinToolchains.loadImplementation(btaClassloader).native.supportedHosts

    @Test
    @DisplayName("Linux x64 is a supported host that links its own target")
    fun testLinuxHost() {
        val linux = hosts.single { it.target == KonanTarget.LINUX_X64 }
        assertTrue(KonanTarget.LINUX_X64 in linux.linkableTargets) { "Unexpected linkable targets: ${linux.linkableTargets}" }
    }

    @Test
    @DisplayName("Every host is listed once and links its own target")
    fun testEveryHost() {
        assertEquals(hosts.size, hosts.distinctBy { it.target }.size) { "Duplicated hosts in $hosts" }
        for (host in hosts) {
            assertTrue(host.target in host.linkableTargets) { "${host.target} cannot link its own target" }
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.nativehost

import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.ARM32
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.ARM64
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.RISCV64
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.X64
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.X86
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.FREEBSD
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.LINUX
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.MACOS
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.UNKNOWN
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.WINDOWS
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInNative
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

@Isolated("The HostManager comparison modifies system properties")
class KotlinNativeHostTest {

    class Expected(
        val os: KotlinNativeHostOs,
        val architecture: KotlinNativeHostArchitecture,
        val name: String,
    )

    companion object {
        @JvmStatic
        fun detectedHosts(): List<Arguments> = listOf(
            Arguments.of("Mac OS X", "aarch64", Expected(MACOS, ARM64, "macos-aarch64")),
            Arguments.of("Mac OS X", "x86_64", Expected(MACOS, X64, "macos-x86_64")),
            Arguments.of("Linux", "amd64", Expected(LINUX, X64, "linux-x86_64")),
            Arguments.of("Linux", "x86_64", Expected(LINUX, X64, "linux-x86_64")),
            Arguments.of("Linux", "aarch64", Expected(LINUX, ARM64, "linux-aarch64")),
            Arguments.of("Linux", "i386", Expected(LINUX, X86, "linux-x86")),
            Arguments.of("Linux", "arm", Expected(LINUX, ARM32, "linux-arm")),
            Arguments.of("Linux", "riscv64", Expected(LINUX, RISCV64, "linux-riscv64")),
            Arguments.of("Linux", "ppc64le", Expected(LINUX, KotlinNativeHostArchitecture.UNKNOWN, "linux-ppc64le")),
            Arguments.of("Windows 11", "amd64", Expected(WINDOWS, X64, "windows-x86_64")),
            Arguments.of("Windows 11", "arm64", Expected(WINDOWS, ARM64, "windows-aarch64")),
            Arguments.of("Windows 11", "x86", Expected(WINDOWS, X86, "windows-x86")),
            Arguments.of("FreeBSD", "amd64", Expected(FREEBSD, X64, "freebsd-x86_64")),
            Arguments.of("SunOS", "aarch64", Expected(UNKNOWN, ARM64, "sunos-aarch64")),
        )

        fun withSystemProperties(osName: String, osArch: String, block: () -> Unit) {
            val backup = mapOf("os.name" to System.getProperty("os.name"), "os.arch" to System.getProperty("os.arch"))
            try {
                System.setProperty("os.name", osName)
                System.setProperty("os.arch", osArch)
                block()
            } finally {
                backup.forEach { (key, value) -> if (value == null) System.clearProperty(key) else System.setProperty(key, value) }
            }
        }
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("detectedHosts")
    fun `detects the host from os name and arch`(osName: String, osArch: String, expected: Expected) {
        val host = KotlinNativeHost.detect(osName, osArch)
        assertEquals(expected.os, host.os)
        assertEquals(expected.architecture, host.architecture)
        assertEquals(expected.name, host.name)
        assertEquals(KotlinNativeHost.of(expected.os, expected.architecture), host)
    }

    @Test
    fun `of produces equal values that survive java serialization`() {
        val host = KotlinNativeHost.of(MACOS, ARM64)
        assertEquals(host, KotlinNativeHost.of(MACOS, ARM64))
        assertEquals(host.hashCode(), KotlinNativeHost.of(MACOS, ARM64).hashCode())

        val restored = serializeAndRestore(host)
        assertEquals(host, restored)
        assertEquals("macos-aarch64", restored.name)
    }

    @MustRunOnChangesInNative
    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("detectedHosts")
    fun `name agrees with HostManager`(osName: String, osArch: String, @Suppress("UNUSED_PARAMETER") expected: Expected) {
        withSystemProperties(osName, osArch) {
            val host = KotlinNativeHost.current()
            // HostManager keeps the case of an unknown `os.name`, the library lowercases it.
            // HostManager also keeps the raw `os.arch` of 32-bit x86 (`i386`, `i686`), the library names them all `x86`.
            val platformName = HostManager.platformName().lowercase()
            assertEquals(if (host.architecture == X86) platformName.replace(osArch, "x86") else platformName, host.name)
        }
    }

    @Test
    fun `unknown name parts are safe to use in file names`() {
        assertEquals("unknown-aarch64", KotlinNativeHost.detect("", "aarch64").name)
        assertEquals("unknown-unknown", KotlinNativeHost.detect(null, null).name)
        assertEquals("z_os-s390x", KotlinNativeHost.detect("z/OS", "s390x").name)
        assertEquals("hp_ux-pa_risc2_0", KotlinNativeHost.detect("HP-UX", "PA_RISC2.0").name)
        assertEquals("unknown-unknown", KotlinNativeHost.of(UNKNOWN, KotlinNativeHostArchitecture.UNKNOWN).name)
    }
}

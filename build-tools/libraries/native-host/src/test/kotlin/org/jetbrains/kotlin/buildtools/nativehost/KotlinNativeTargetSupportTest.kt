/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.nativehost

import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KonanTarget
import org.jetbrains.kotlin.buildtools.api.konan.NativeHost
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.ARM64
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.X64
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.LINUX
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.MACOS
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.UNKNOWN
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInNative
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Isolated("The HostManager comparison modifies system properties")
@OptIn(DelicateKotlinNativeHostApi::class, ExperimentalBuildToolsApi::class, DeprecatedCompilerArgument::class)
class KotlinNativeTargetSupportTest {

    companion object {
        @JvmStatic
        fun detectedHosts() = KotlinNativeHostTest.detectedHosts()
    }

    private val bundled = KotlinNativeTargetSupport.bundled()

    private val linuxX64 = KotlinNativeHost.of(LINUX, X64)
    private val linuxArm64 = KotlinNativeHost.of(LINUX, ARM64)
    private val macosX64 = KotlinNativeHost.of(MACOS, X64)
    private val macosArm64 = KotlinNativeHost.of(MACOS, ARM64)

    @Test
    fun `an unsupported host has no target and links nothing`() {
        listOf(linuxArm64, macosX64, KotlinNativeHost.of(UNKNOWN, KotlinNativeHostArchitecture.UNKNOWN)).forEach { host ->
            assertFalse(host in bundled.supportedHosts, host.name)
            assertNull(bundled.target(host), host.name)
            KonanTarget.entries.forEach { target ->
                assertFalse(bundled.canLinkBinaries(host, target), "${host.name} must not link $target")
            }
        }
    }

    @Test
    fun `the data of a toolchain is used`() {
        // A Kotlin/Native version that still has the Intel Mac host and already has a Linux ARM64 host.
        val support = supportOf(
            FakeNativeHost(KonanTarget.MACOS_X64, setOf(KonanTarget.MACOS_X64, KonanTarget.IOS_X64, KonanTarget.LINUX_X64)),
            FakeNativeHost(KonanTarget.LINUX_ARM64, setOf(KonanTarget.LINUX_ARM64, KonanTarget.LINUX_X64)),
        )

        assertEquals(listOf(macosX64, linuxArm64), support.supportedHosts)
        assertEquals(KonanTarget.MACOS_X64, support.target(macosX64))
        assertEquals(KonanTarget.LINUX_ARM64, support.target(linuxArm64))
        assertTrue(support.canLinkBinaries(macosX64, KonanTarget.IOS_X64))
        assertFalse(support.canLinkBinaries(linuxArm64, KonanTarget.IOS_X64))

        // The hosts of the bundled data are not supported by this version.
        assertNull(support.target(macosArm64))
        assertFalse(support.canLinkBinaries(linuxX64, KonanTarget.LINUX_X64))
    }

    @Test
    fun `a toolchain host with a target the library does not know as a host is left out`() {
        val support = supportOf(
            FakeNativeHost(KonanTarget.LINUX_X64, setOf(KonanTarget.LINUX_X64)),
            FakeNativeHost(KonanTarget.ANDROID_X64, setOf(KonanTarget.ANDROID_X64)),
        )
        assertEquals(listOf(linuxX64), support.supportedHosts)
    }

    @Test
    fun `inconsistent toolchain data does not throw`() {
        val support = supportOf(
            // The host target is missing from the linkable targets.
            FakeNativeHost(KonanTarget.LINUX_X64, setOf(KonanTarget.MINGW_X64)),
            // The same host is listed again.
            FakeNativeHost(KonanTarget.LINUX_X64, setOf(KonanTarget.LINUX_ARM64)),
        )
        assertEquals(listOf(linuxX64), support.supportedHosts)
        assertEquals(KonanTarget.LINUX_X64, support.target(linuxX64))
        assertTrue(support.canLinkBinaries(linuxX64, KonanTarget.LINUX_X64))
        assertTrue(support.canLinkBinaries(linuxX64, KonanTarget.MINGW_X64))
        assertFalse(support.canLinkBinaries(linuxX64, KonanTarget.LINUX_ARM64))
    }

    @Test
    fun `values are equal by data in any host order and survive java serialization`() {
        val linux = FakeNativeHost(KonanTarget.LINUX_X64, setOf(KonanTarget.LINUX_X64, KonanTarget.MINGW_X64))
        val mac = FakeNativeHost(KonanTarget.MACOS_ARM64, setOf(KonanTarget.MACOS_ARM64))
        val support = supportOf(linux, mac)

        assertEquals(support, supportOf(mac, linux))
        assertEquals(support.hashCode(), supportOf(mac, linux).hashCode())
        assertEquals(support.supportedHosts, supportOf(mac, linux).supportedHosts)
        assertNotEquals(support, supportOf(linux))

        assertEquals(support, serializeAndRestore(support))
        assertEquals(bundled, serializeAndRestore(bundled))
    }

    @MustRunOnChangesInNative
    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("detectedHosts")
    fun `bundled host target agrees with HostManager`(
        osName: String,
        osArch: String,
        @Suppress("UNUSED_PARAMETER") expected: KotlinNativeHostTest.Expected,
    ) {
        KotlinNativeHostTest.withSystemProperties(osName, osArch) {
            val host = KotlinNativeHost.current()
            assertEquals(HostManager.hostOrNull?.name, bundled.target(host)?.stringValue)
            assertEquals(HostManager.hostIsSupported, host in bundled.supportedHosts)
        }
    }

    @MustRunOnChangesInNative
    @Test
    fun `bundled data equals the data of HostManager`() {
        // HostManager described as a toolchain would describe it.
        val hostsOfHostManager = HostManager().enabledByHost.map { (host, targets) ->
            FakeNativeHost(btaTarget(host.name), targets.mapTo(mutableSetOf()) { btaTarget(it.name) })
        }
        val fromHostManager = supportOf(*hostsOfHostManager.toTypedArray())

        // Fails on a host of HostManager that the library does not know as a host target yet.
        assertEquals(hostsOfHostManager.map { it.target }.toSet(), fromHostManager.supportedHosts.map { fromHostManager.target(it) }.toSet())
        assertEquals(fromHostManager, bundled)
        assertEquals(fromHostManager.supportedHosts, bundled.supportedHosts)
    }

    private fun btaTarget(name: String): KonanTarget = KonanTarget.entries.single { it.stringValue == name }
}

@OptIn(ExperimentalBuildToolsApi::class)
private fun supportOf(vararg hosts: NativeHost): KotlinNativeTargetSupport = KotlinNativeTargetSupport.of(FakeNativeToolchain(*hosts))

@OptIn(ExperimentalBuildToolsApi::class)
private class FakeNativeToolchain(vararg hosts: NativeHost) : NativePlatformToolchain {
    override val supportedHosts: List<NativeHost> = hosts.toList()

    override fun nativeKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): NativeKlibCompilationOperation.Builder =
        error("Not used by the tests")
}

@OptIn(ExperimentalBuildToolsApi::class)
private class FakeNativeHost(
    override val target: KonanTarget,
    override val linkableTargets: Set<KonanTarget>,
) : NativeHost

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.nativehost

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KonanTarget
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain
import java.io.Serializable

/**
 * What one Kotlin/Native version supports: the hosts it has a distribution for and the targets each of them can link.
 *
 * The answers differ between Kotlin/Native versions, so an instance holds the data of one version. [of] reads it from
 * the Build Tools API toolchain of the version the build uses. [bundled] is the copy that ships with this library.
 *
 * Targets are entries of the Build Tools API [KonanTarget] enum. No function throws: an unsupported host gives
 * `false` or `null`.
 */
public class KotlinNativeTargetSupport private constructor(
    // Targets are stored by name, so that the value does not depend on the entries of the enum when it is restored.
    private val targetNames: Map<KotlinNativeHost, String>,
    private val linkableTargets: Map<KotlinNativeHost, Set<String>>,
) : Serializable {

    /**
     * The hosts that have a Kotlin/Native distribution, in the order of the [KotlinNativeHostOs] entries.
     * `host in supportedHosts` tells whether a host is supported. Same as `HostManager.hostIsSupported`.
     */
    public val supportedHosts: List<KotlinNativeHost> get() = targetNames.keys.toList()

    /**
     * The Kotlin/Native target of the [host], e.g. [KonanTarget.MACOS_ARM64], or `null` if the host is not supported.
     * Same as `HostManager.hostOrNull`.
     *
     * Binaries of this target, such as tests, are the ones that run on the host itself.
     */
    public fun target(host: KotlinNativeHost): KonanTarget? =
        targetNames[host]?.let { name -> KonanTarget.entries.firstOrNull { it.stringValue == name } }

    /**
     * Whether the [host] can link binaries for the [target], e.g. [KonanTarget.IOS_ARM64]. Apple targets need
     * a macOS host. Same as `HostManager().isEnabled(target)`, except that an unsupported host gives `false` and no
     * exception.
     */
    public fun canLinkBinaries(host: KotlinNativeHost, target: KonanTarget): Boolean =
        linkableTargets[host]?.contains(target.stringValue) == true

    override fun equals(other: Any?): Boolean =
        other is KotlinNativeTargetSupport && targetNames == other.targetNames && linkableTargets == other.linkableTargets

    override fun hashCode(): Int = 31 * targetNames.hashCode() + linkableTargets.hashCode()

    override fun toString(): String = "KotlinNativeTargetSupport(hosts=${targetNames.values})"

    /** Factories of [KotlinNativeTargetSupport]. */
    @OptIn(ExperimentalBuildToolsApi::class)
    public companion object {
        @Suppress("unused") // read by Java serialization
        private const val serialVersionUID = 1L

        /**
         * The support data of the Kotlin/Native version this library was released with, copied from `HostManager`.
         * It can be wrong if the build uses another Kotlin/Native version, so prefer [of] when the build tool has
         * the Build Tools API toolchain of that version.
         */
        @JvmStatic
        @DelicateKotlinNativeHostApi
        public fun bundled(): KotlinNativeTargetSupport = bundled

        /**
         * The support data of the Kotlin/Native version behind the [toolchain].
         *
         * A host whose target this library does not know as a host target is left out, so it counts as unsupported.
         */
        @JvmStatic
        @ExperimentalBuildToolsApi
        public fun of(toolchain: NativePlatformToolchain): KotlinNativeTargetSupport {
            val targetNames = LinkedHashMap<KotlinNativeHost, String>()
            val linkableTargets = LinkedHashMap<KotlinNativeHost, Set<String>>()
            toolchain.supportedHosts.mapNotNull { data -> hostsByTargetName[data.target.stringValue]?.let { host -> host to data } }
                // Sorted, so that equal values list their hosts in the same order.
                .sortedWith(compareBy({ it.first.os }, { it.first.architecture }))
                .forEach { hostAndData ->
                    val host = hostAndData.first
                    val data = hostAndData.second
                    // The first entry of a host wins. A correct toolchain lists a host once.
                    if (host !in targetNames) {
                        targetNames[host] = data.target.stringValue
                        // A host always links its own target.
                        linkableTargets[host] = (data.linkableTargets + data.target).mapTo(LinkedHashSet()) { it.stringValue }
                    }
                }
            return KotlinNativeTargetSupport(targetNames, linkableTargets)
        }

        // Copied from HostManager.enabledByHost in kotlin-native-utils; KotlinNativeTargetSupportTest checks they agree.
        private val commonTargets: Set<String> = setOf(
            "linux_x64",
            "linux_arm32_hfp",
            "linux_arm64",
            "mingw_x64",
            "android_x86",
            "android_x64",
            "android_arm32",
            "android_arm64",
        )

        private val appleTargets: Set<String> = setOf(
            "macos_arm64",
            "ios_arm64",
            "ios_x64",
            "ios_simulator_arm64",
            "watchos_arm64",
            "watchos_x64",
            "watchos_simulator_arm64",
            "watchos_device_arm64",
            "tvos_arm64",
            "tvos_x64",
            "tvos_simulator_arm64",
        )

        private val macosArm64 = KotlinNativeHost.of(KotlinNativeHostOs.MACOS, KotlinNativeHostArchitecture.ARM64)
        private val linuxX64 = KotlinNativeHost.of(KotlinNativeHostOs.LINUX, KotlinNativeHostArchitecture.X64)
        private val windowsX64 = KotlinNativeHost.of(KotlinNativeHostOs.WINDOWS, KotlinNativeHostArchitecture.X64)

        private val macosX64 = KotlinNativeHost.of(KotlinNativeHostOs.MACOS, KotlinNativeHostArchitecture.X64)
        private val linuxArm64 = KotlinNativeHost.of(KotlinNativeHostOs.LINUX, KotlinNativeHostArchitecture.ARM64)

        // The machine a host target runs on. It covers the bundled hosts and those of other Kotlin/Native versions:
        // the Intel Mac of older ones, and Linux ARM64. A toolchain host with another target is left out.
        private val hostsByTargetName: Map<String, KotlinNativeHost> = mapOf(
            "macos_arm64" to macosArm64,
            "macos_x64" to macosX64,
            "linux_x64" to linuxX64,
            "linux_arm64" to linuxArm64,
            "mingw_x64" to windowsX64,
        )

        // Targets are names here, so that the table loads with any version of the BTA API. The hosts are in the order
        // `of` gives them: by OS and architecture.
        private val bundled = KotlinNativeTargetSupport(
            targetNames = linkedMapOf(macosArm64 to "macos_arm64", linuxX64 to "linux_x64", windowsX64 to "mingw_x64"),
            linkableTargets = linkedMapOf(
                macosArm64 to commonTargets + appleTargets,
                linuxX64 to commonTargets,
                windowsX64 to commonTargets,
            ),
        )
    }
}

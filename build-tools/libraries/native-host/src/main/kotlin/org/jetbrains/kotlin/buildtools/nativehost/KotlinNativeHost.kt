/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.nativehost

import java.io.Serializable
import java.util.Locale

/**
 * The machine the build runs on, as Kotlin/Native sees it.
 *
 * Built from the `os.name` and `os.arch` system properties. It never throws: on a host without a Kotlin/Native
 * distribution, [isSupported] is `false`, [targetName] is `null` and the `can*` checks return `false`.
 *
 * Targets are named as Kotlin/Native names them, for example `macos_arm64` or `linux_x64`.
 *
 * Two hosts are equal when their [os] and [architecture] are equal. The value is serializable, so read it once and
 * pass it on.
 */
public interface KotlinNativeHost : Serializable {

    /** The operating system of the host. */
    public val os: KotlinNativeHostOs

    /** The CPU architecture of the host. */
    public val architecture: KotlinNativeHostArchitecture

    /**
     * Whether a Kotlin/Native distribution exists for this host.
     *
     * See [the list of supported hosts](https://kotlinlang.org/docs/native-target-support.html#hosts).
     */
    public val isSupported: Boolean

    /** The Kotlin/Native target whose binaries run on this host, for example `macos_arm64`, or `null` if unsupported. */
    public val targetName: String?

    /**
     * A stable host id in the form `<os>-<architecture>`, for example `macos-aarch64`.
     *
     * On supported hosts it is the classifier of the `kotlin-native-prebuilt` distribution. Unknown parts fall back to
     * the raw `os.name` or `os.arch` value.
     */
    public val name: String

    /** `true` if [os] is [KotlinNativeHostOs.MACOS]. */
    public val isMacOs: Boolean

    /** `true` if [os] is [KotlinNativeHostOs.LINUX]. */
    public val isLinux: Boolean

    /** `true` if [os] is [KotlinNativeHostOs.WINDOWS]. */
    public val isWindows: Boolean

    /**
     * Whether this host can link binaries for the target named [targetName]. Linux and Windows hosts link non-Apple
     * targets only. Unknown target names give `false`.
     */
    public fun canLinkBinaries(targetName: String): Boolean

    /**
     * Whether binaries of the target named [targetName], such as test executables, can run on this host.
     *
     * `true` for the host target and, on macOS, for simulator targets with the host architecture.
     */
    public fun canRunBinaries(targetName: String): Boolean

    public companion object {
        /** The host of the current build, read from the `os.name` and `os.arch` system properties. */
        @JvmStatic
        public fun current(): KotlinNativeHost {
            val osName = System.getProperty("os.name").orEmpty()
            val osArch = System.getProperty("os.arch").orEmpty()
            return DefaultKotlinNativeHost(
                os = KotlinNativeHostOs.detect(osName),
                architecture = KotlinNativeHostArchitecture.detect(osArch),
                rawOsName = osName,
                rawOsArch = osArch,
            )
        }

        /** A host with the given [os] and [architecture], for testing host-dependent build logic. */
        @JvmStatic
        public fun of(os: KotlinNativeHostOs, architecture: KotlinNativeHostArchitecture): KotlinNativeHost =
            DefaultKotlinNativeHost(os, architecture, rawOsName = os.name, rawOsArch = architecture.name)

        /** The hosts Kotlin/Native ships a distribution for. */
        @JvmStatic
        public fun supportedHosts(): List<KotlinNativeHost> = listOf(
            of(KotlinNativeHostOs.MACOS, KotlinNativeHostArchitecture.ARM64),
            of(KotlinNativeHostOs.LINUX, KotlinNativeHostArchitecture.X64),
            of(KotlinNativeHostOs.WINDOWS, KotlinNativeHostArchitecture.X64),
        )
    }
}

/** Operating systems Kotlin/Native distinguishes for the build host. */
public enum class KotlinNativeHostOs {
    /** `os.name` is `Mac OS X`. */
    MACOS,

    /** `os.name` is `Linux`. */
    LINUX,

    /** `os.name` starts with `Windows`. */
    WINDOWS,

    /** Anything else, for example FreeBSD. Kotlin/Native has no distribution for it. */
    UNKNOWN;

    internal companion object {
        internal fun detect(osName: String): KotlinNativeHostOs = when {
            osName == "Mac OS X" -> MACOS
            osName == "Linux" -> LINUX
            osName.startsWith("Windows") -> WINDOWS
            else -> UNKNOWN
        }
    }
}

/** CPU architectures Kotlin/Native distinguishes for the build host. */
public enum class KotlinNativeHostArchitecture {
    /** `os.arch` is `x86_64` or `amd64`. */
    X64,

    /** `os.arch` is `aarch64` or `arm64`. */
    ARM64,

    /** Anything else, for example riscv64. Kotlin/Native has no distribution for it. */
    UNKNOWN;

    internal companion object {
        internal fun detect(osArch: String): KotlinNativeHostArchitecture = when (osArch) {
            "x86_64", "amd64" -> X64
            "arm64", "aarch64" -> ARM64
            else -> UNKNOWN
        }
    }
}

private class DefaultKotlinNativeHost(
    override val os: KotlinNativeHostOs,
    override val architecture: KotlinNativeHostArchitecture,
    rawOsName: String,
    rawOsArch: String,
) : KotlinNativeHost {

    // State is two enums and two strings, so the value serializes anywhere.
    override val targetName: String?
        get() = when (os to architecture) {
            KotlinNativeHostOs.MACOS to KotlinNativeHostArchitecture.ARM64 -> "macos_arm64"
            KotlinNativeHostOs.LINUX to KotlinNativeHostArchitecture.X64 -> "linux_x64"
            KotlinNativeHostOs.WINDOWS to KotlinNativeHostArchitecture.X64 -> "mingw_x64"
            else -> null
        }

    override val isSupported: Boolean get() = targetName != null

    // Each part falls back on its own: a known OS keeps its name next to an unknown architecture (`windows-riscv64`).
    override val name: String = listOf(
        os.distributionName ?: rawOsName.lowercase(Locale.ROOT),
        architecture.distributionName ?: rawOsArch.lowercase(Locale.ROOT),
    ).joinToString("-")

    override val isMacOs: Boolean get() = os == KotlinNativeHostOs.MACOS
    override val isLinux: Boolean get() = os == KotlinNativeHostOs.LINUX
    override val isWindows: Boolean get() = os == KotlinNativeHostOs.WINDOWS

    override fun canLinkBinaries(targetName: String): Boolean {
        val host = this.targetName ?: return false
        return targetName in linkableTargetsByHost.getValue(host)
    }

    override fun canRunBinaries(targetName: String): Boolean {
        val host = this.targetName ?: return false
        if (targetName == host) return true
        return isMacOs && simulatorArchitectures[targetName] == architecture
    }

    override fun equals(other: Any?): Boolean =
        other is DefaultKotlinNativeHost && os == other.os && architecture == other.architecture

    override fun hashCode(): Int = 31 * os.hashCode() + architecture.hashCode()

    override fun toString(): String = "KotlinNativeHost($name)"

    private val KotlinNativeHostOs.distributionName: String?
        get() = when (this) {
            KotlinNativeHostOs.MACOS -> "macos"
            KotlinNativeHostOs.LINUX -> "linux"
            KotlinNativeHostOs.WINDOWS -> "windows"
            KotlinNativeHostOs.UNKNOWN -> null
        }

    private val KotlinNativeHostArchitecture.distributionName: String?
        get() = when (this) {
            KotlinNativeHostArchitecture.X64 -> "x86_64"
            KotlinNativeHostArchitecture.ARM64 -> "aarch64"
            KotlinNativeHostArchitecture.UNKNOWN -> null
        }

    companion object {
        @Suppress("unused") // read by Java serialization
        private const val serialVersionUID = 1L

        // Targets each supported host can link. Copied from HostManager.enabledByHost in kotlin-native-utils;
        // KotlinNativeHostBridgeTest in kotlin-gradle-plugin checks they agree.
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

        private val linkableTargetsByHost: Map<String, Set<String>> = mapOf(
            "linux_x64" to commonTargets,
            "mingw_x64" to commonTargets,
            "macos_arm64" to commonTargets + appleTargets,
        )

        // Apple simulator targets and the host architecture they run on.
        private val simulatorArchitectures: Map<String, KotlinNativeHostArchitecture> = mapOf(
            "ios_x64" to KotlinNativeHostArchitecture.X64,
            "ios_simulator_arm64" to KotlinNativeHostArchitecture.ARM64,
            "watchos_x64" to KotlinNativeHostArchitecture.X64,
            "watchos_simulator_arm64" to KotlinNativeHostArchitecture.ARM64,
            "tvos_x64" to KotlinNativeHostArchitecture.X64,
            "tvos_simulator_arm64" to KotlinNativeHostArchitecture.ARM64,
        )
    }
}

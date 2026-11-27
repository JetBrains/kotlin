/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.nativehost

import java.io.Serializable
import java.util.Locale

/**
 * The machine the build runs on, as the JVM reports it: its [os] and [architecture].
 *
 * Nothing here throws, also on hosts without a Kotlin/Native distribution such as Linux ARM64, where `HostManager`
 * from `kotlin-native-utils` does. What Kotlin/Native supports on a host depends on the Kotlin/Native version,
 * see [KotlinNativeTargetSupport].
 *
 * Hosts are equal if their [os] and [architecture] are equal.
 *
 * Get instances from [current] or [of].
 */
public sealed interface KotlinNativeHost : Serializable {

    /** The operating system of the host. */
    public val os: KotlinNativeHostOs

    /** The CPU architecture of the host. */
    public val architecture: KotlinNativeHostArchitecture

    /**
     * A host id in the form `<os>-<architecture>`, e.g. `macos-aarch64` or `freebsd-x86_64`.
     *
     * On hosts with a Kotlin/Native distribution it is the classifier of `kotlin-native-prebuilt`.
     *
     * Don't parse the name. For an `UNKNOWN` OS or architecture the part is the `os.name` or `os.arch` value in lower
     * case, with every character other than a letter, a digit or `_` replaced by `_`. It is `unknown` if that value
     * is empty or the host was created with [of].
     */
    public val name: String

    /** Factories of [KotlinNativeHost]. */
    public companion object {
        /**
         * The current host, detected from the `os.name` and `os.arch` system properties. These describe the running
         * JVM: an x64 JVM on an Apple Silicon Mac gives [KotlinNativeHostArchitecture.X64].
         */
        @JvmStatic
        public fun current(): KotlinNativeHost = detect(System.getProperty("os.name"), System.getProperty("os.arch"))

        // Detection without system properties, so that tests don't have to change them.
        internal fun detect(osName: String?, osArch: String?): KotlinNativeHost = DefaultKotlinNativeHost(
            os = KotlinNativeHostOs.detect(osName.orEmpty()),
            architecture = KotlinNativeHostArchitecture.detect(osArch.orEmpty()),
            rawOsName = osName.orEmpty(),
            rawOsArch = osArch.orEmpty(),
        )

        /** A host with the given [os] and [architecture]. Useful for testing host-dependent logic. */
        @JvmStatic
        public fun of(os: KotlinNativeHostOs, architecture: KotlinNativeHostArchitecture): KotlinNativeHost =
            DefaultKotlinNativeHost(os, architecture, rawOsName = "", rawOsArch = "")
    }
}

/**
 * Host operating system. Covers the common ones, including those Kotlin/Native does not support.
 *
 * New entries can be added, so keep an `else` branch in `when`.
 */
public enum class KotlinNativeHostOs {
    /** `os.name` is `Mac OS X`. */
    MACOS,

    /** `os.name` is `Linux`. */
    LINUX,

    /** `os.name` starts with `Windows`. */
    WINDOWS,

    /** `os.name` is `FreeBSD`. */
    FREEBSD,

    /** Anything else, for example Solaris. */
    UNKNOWN;

    internal companion object {
        internal fun detect(osName: String): KotlinNativeHostOs = when {
            osName == "Mac OS X" -> MACOS
            osName == "Linux" -> LINUX
            osName.startsWith("Windows") -> WINDOWS
            osName == "FreeBSD" -> FREEBSD
            else -> UNKNOWN
        }
    }
}

/**
 * Host CPU architecture. Covers the common ones, including those Kotlin/Native does not support.
 *
 * New entries can be added, so keep an `else` branch in `when`.
 */
public enum class KotlinNativeHostArchitecture {
    /** `os.arch` is `x86`, `i386`, `i486`, `i586` or `i686`. */
    X86,

    /** `os.arch` is `x86_64` or `amd64`. */
    X64,

    /** `os.arch` is `arm`. */
    ARM32,

    /** `os.arch` is `aarch64` or `arm64`. */
    ARM64,

    /** `os.arch` is `riscv64`. */
    RISCV64,

    /** Anything else, for example ppc64le. */
    UNKNOWN;

    internal companion object {
        internal fun detect(osArch: String): KotlinNativeHostArchitecture = when (osArch) {
            "x86", "i386", "i486", "i586", "i686" -> X86
            "x86_64", "amd64" -> X64
            "arm" -> ARM32
            "arm64", "aarch64" -> ARM64
            "riscv64" -> RISCV64
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

    // The only stored state is the two enums and `name`, so the value serializes anywhere.
    // Each part falls back on its own: a known OS keeps its name next to an unknown architecture (`linux-ppc64le`).
    override val name: String = listOf(
        os.namePart ?: rawOsName.toNamePart(),
        architecture.namePart ?: rawOsArch.toNamePart(),
    ).joinToString("-")

    override fun equals(other: Any?): Boolean =
        other is DefaultKotlinNativeHost && os == other.os && architecture == other.architecture

    // Ordinals, because enum hash codes differ between JVM runs.
    override fun hashCode(): Int = 31 * os.ordinal + architecture.ordinal

    override fun toString(): String = "KotlinNativeHost($name)"

    // A raw `os.name` or `os.arch` value can be empty or contain characters like `/` (`z/OS`).
    private fun String.toNamePart(): String =
        lowercase(Locale.ROOT).replace(unsafeNameCharacters, "_").ifEmpty { "unknown" }

    // macOS, Linux, Windows, x86_64 and aarch64 are named as in the Kotlin/Native distribution classifier.
    private val KotlinNativeHostOs.namePart: String?
        get() = when (this) {
            KotlinNativeHostOs.MACOS -> "macos"
            KotlinNativeHostOs.LINUX -> "linux"
            KotlinNativeHostOs.WINDOWS -> "windows"
            KotlinNativeHostOs.FREEBSD -> "freebsd"
            KotlinNativeHostOs.UNKNOWN -> null
        }

    private val KotlinNativeHostArchitecture.namePart: String?
        get() = when (this) {
            KotlinNativeHostArchitecture.X86 -> "x86"
            KotlinNativeHostArchitecture.X64 -> "x86_64"
            KotlinNativeHostArchitecture.ARM32 -> "arm"
            KotlinNativeHostArchitecture.ARM64 -> "aarch64"
            KotlinNativeHostArchitecture.RISCV64 -> "riscv64"
            KotlinNativeHostArchitecture.UNKNOWN -> null
        }

    companion object {
        @Suppress("unused") // read by Java serialization
        private const val serialVersionUID = 1L

        private val unsafeNameCharacters = Regex("[^a-z0-9_]+")
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata

@Serializable
enum class KonanTarget(
    val targetName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    @SerialName("android_x64")
    ANDROID_X64(
        targetName = "android_x64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("android_x86")
    ANDROID_X86(
        targetName = "android_x86",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("android_arm32")
    ANDROID_ARM32(
        targetName = "android_arm32",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("android_arm64")
    ANDROID_ARM64(
        targetName = "android_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("ios_arm64")
    IOS_ARM64(
        targetName = "ios_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("ios_x64")
    IOS_X64(
        targetName = "ios_x64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("ios_simulator_arm64")
    IOS_SIMULATOR_ARM64(
        targetName = "ios_simulator_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("watchos_arm32")
    WATCHOS_ARM32(
        targetName = "watchos_arm32",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("watchos_arm64")
    WATCHOS_ARM64(
        targetName = "watchos_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("watchos_x64")
    WATCHOS_X64(
        targetName = "watchos_x64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v2_3_20,
        )
    ),

    @SerialName("watchos_simulator_arm64")
    WATCHOS_SIMULATOR_ARM64(
        targetName = "watchos_simulator_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("watchos_device_arm64")
    WATCHOS_DEVICE_ARM64(
        targetName = "watchos_device_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_0,
            stabilizedVersion = KotlinReleaseVersion.v1_8_0,
        )
    ),

    @SerialName("tvos_arm64")
    TVOS_ARM64(
        targetName = "tvos_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("tvos_x64")
    TVOS_X64(
        targetName = "tvos_x64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v2_3_20,
        )
    ),

    @SerialName("tvos_simulator_arm64")
    TVOS_SIMULATOR_ARM64(
        targetName = "tvos_simulator_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("linux_x64")
    LINUX_X64(
        targetName = "linux_x64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("mingw_x64")
    MINGW_X64(
        targetName = "mingw_x64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("macos_x64")
    MACOS_X64(
        targetName = "macos_x64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v2_3_20,
        )
    ),

    @SerialName("macos_arm64")
    MACOS_ARM64(
        targetName = "macos_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("linux_arm64")
    LINUX_ARM64(
        targetName = "linux_arm64",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("linux_arm32_hfp")
    LINUX_ARM32_HFP(
        targetName = "linux_arm32_hfp",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v1_8_20,
        )
    ),

    @SerialName("ios_arm32")
    IOS_ARM32(
        targetName = "ios_arm32",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v1_8_20,
            removedVersion = KotlinReleaseVersion.v1_9_20,
        )
    ),

    @SerialName("watchos_x86")
    WATCHOS_X86(
        targetName = "watchos_x86",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v1_8_20,
            removedVersion = KotlinReleaseVersion.v1_9_20,
        )
    ),

    @SerialName("linux_mips32")
    LINUX_MIPS32(
        targetName = "linux_mips32",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v1_8_20,
            removedVersion = KotlinReleaseVersion.v1_9_20,
        )
    ),

    @SerialName("linux_mipsel32")
    LINUX_MIPSEL32(
        targetName = "linux_mipsel32",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v1_8_20,
            removedVersion = KotlinReleaseVersion.v1_9_20,
        )
    ),

    @SerialName("mingw_x86")
    MINGW_X86(
        targetName = "mingw_x86",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v1_8_20,
            removedVersion = KotlinReleaseVersion.v1_9_20,
        )
    ),

    @SerialName("wasm32")
    WASM32(
        targetName = "wasm32",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
            stabilizedVersion = KotlinReleaseVersion.v1_5_20,
            deprecatedVersion = KotlinReleaseVersion.v1_8_20,
            removedVersion = KotlinReleaseVersion.v1_9_20,
        )
    );

    override val stringRepresentation: String
        get() = targetName
}

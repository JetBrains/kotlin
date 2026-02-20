/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.serialization.json.KotlinJvmTargetAsNameSerializer

@Serializable(with = KotlinJvmTargetAsNameSerializer::class)
enum class JvmTarget(
    val targetName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    jvm1_6(
        targetName = "1.6",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
            deprecatedVersion = KotlinReleaseVersion.v1_5_0,
            removedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    jvm1_8(
        targetName = "1.8",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
        )
    ),
    jvm_9(
        targetName = "9",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_30,
            stabilizedVersion = KotlinReleaseVersion.v1_3_30,
        )
    ),
    jvm_10(
        targetName = "10",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_30,
            stabilizedVersion = KotlinReleaseVersion.v1_3_30,
        )
    ),
    jvm_11(
        targetName = "11",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_30,
            stabilizedVersion = KotlinReleaseVersion.v1_3_30,
        )
    ),
    jvm_12(
        targetName = "12",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_30,
            stabilizedVersion = KotlinReleaseVersion.v1_3_30,
        )
    ),
    jvm_13(
        targetName = "13",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
            stabilizedVersion = KotlinReleaseVersion.v1_3_70,
        )
    ),
    jvm_14(
        targetName = "14",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
        )
    ),
    jvm_15(
        targetName = "15",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
            stabilizedVersion = KotlinReleaseVersion.v1_4_20,
        )
    ),
    jvm_16(
        targetName = "16",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
            stabilizedVersion = KotlinReleaseVersion.v1_5_0,
        )
    ),
    jvm_17(
        targetName = "17",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_0,
            stabilizedVersion = KotlinReleaseVersion.v1_6_0,
        )
    ),
    jvm_18(
        targetName = "18",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_20,
            stabilizedVersion = KotlinReleaseVersion.v1_6_20,
        )
    ),
    jvm_19(
        targetName = "19",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_0,
            stabilizedVersion = KotlinReleaseVersion.v1_8_0,
        )
    ),
    jvm_20(
        targetName = "20",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
            stabilizedVersion = KotlinReleaseVersion.v1_9_0,
        )
    ),
    jvm_21(
        targetName = "21",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20,
            stabilizedVersion = KotlinReleaseVersion.v1_9_20,
        )
    ),
    jvm_22(
        targetName = "22",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
            stabilizedVersion = KotlinReleaseVersion.v2_0_0,
        )
    ),
    jvm_23(
        targetName = "23",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
            stabilizedVersion = KotlinReleaseVersion.v2_1_0,
        )
    ),
    jvm_24(
        targetName = "24",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
            stabilizedVersion = KotlinReleaseVersion.v2_2_0,
        )
    ),
    jvm_25(
        targetName = "25",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0,
            stabilizedVersion = KotlinReleaseVersion.v2_3_0,
        )
    ),
    ;

    companion object {
        internal val CURRENT_SUPPORTED_VERSIONS_DESCRIPTION =
            "${jvm1_8.targetName} and ${jvm_9.targetName}–${JvmTarget.entries.last().targetName}"
        internal val CURRENT_DEFAULT_VERSION = jvm1_8.targetName
    }

    override val stringRepresentation: String
        get() = targetName
}

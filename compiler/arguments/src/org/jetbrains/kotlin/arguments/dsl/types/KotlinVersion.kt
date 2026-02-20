/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.serialization.json.KotlinVersionAsNameSerializer

@Suppress("EnumEntryName")
@Serializable(with = KotlinVersionAsNameSerializer::class)
enum class KotlinVersion(
    val versionName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    v1_0(
        versionName = "1.0",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
            deprecatedVersion = KotlinReleaseVersion.v1_3_0,
            removedVersion = KotlinReleaseVersion.v1_4_0,
        )
    ),
    v1_1(
        versionName = "1.1",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_1_0,
            deprecatedVersion = KotlinReleaseVersion.v1_3_0,
            removedVersion = KotlinReleaseVersion.v1_4_0,
        )
    ),
    v1_2(
        versionName = "1.2",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_2,
            stabilizedVersion = KotlinReleaseVersion.v1_2_0,
            deprecatedVersion = KotlinReleaseVersion.v1_4_0,
            removedVersion = KotlinReleaseVersion.v1_5_0,
        )
    ),
    v1_3(
        versionName = "1.3",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_50,
            stabilizedVersion = KotlinReleaseVersion.v1_3_0,
            deprecatedVersion = KotlinReleaseVersion.v1_5_0,
            removedVersion = KotlinReleaseVersion.v1_9_0,
        )
    ),
    v1_4(
        versionName = "1.4",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
            deprecatedVersion = KotlinReleaseVersion.v1_6_0,
            removedVersion = KotlinReleaseVersion.v2_1_0,
        )
    ),
    v1_5(
        versionName = "1.5",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_5_0,
            deprecatedVersion = KotlinReleaseVersion.v1_9_0,
            removedVersion = KotlinReleaseVersion.v2_1_0,
        )
    ),
    v1_6(
        versionName = "1.6",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
            stabilizedVersion = KotlinReleaseVersion.v1_6_0,
            deprecatedVersion = KotlinReleaseVersion.v1_3_0,
            removedVersion = KotlinReleaseVersion.v2_1_0,
        )
    ),
    v1_7(
        versionName = "1.7",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
            deprecatedVersion = KotlinReleaseVersion.v1_3_0,
            removedVersion = KotlinReleaseVersion.v2_2_0,
        )
    ),
    v1_8(
        versionName = "1.8",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_20,
            stabilizedVersion = KotlinReleaseVersion.v1_8_0,
            deprecatedVersion = KotlinReleaseVersion.v2_2_0,
            removedVersion = KotlinReleaseVersion.v2_3_0,
        )
    ),
    v1_9(
        versionName = "1.9",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_0,
            stabilizedVersion = KotlinReleaseVersion.v1_9_0,
            deprecatedVersion = KotlinReleaseVersion.v2_2_0,
        )
    ),
    v2_0(
        versionName = "2.0",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
            stabilizedVersion = KotlinReleaseVersion.v2_0_0,
            deprecatedVersion = KotlinReleaseVersion.v2_3_0,
        )
    ),
    v2_1(
        versionName = "2.1",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
            stabilizedVersion = KotlinReleaseVersion.v2_1_0,
        )
    ),
    v2_2(
        versionName = "2.2",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
            stabilizedVersion = KotlinReleaseVersion.v2_2_0,
        )
    ),
    v2_3(
        versionName = "2.3",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
            stabilizedVersion = KotlinReleaseVersion.v2_3_0,
        )
    ),
    v2_4(
        versionName = "2.4",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20,
            stabilizedVersion = KotlinReleaseVersion.v2_4_0,
        )
    ),
    v2_5(
        versionName = "2.5",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20,
        )
    ),
    ;

    override val stringRepresentation: String
        get() = versionName
}

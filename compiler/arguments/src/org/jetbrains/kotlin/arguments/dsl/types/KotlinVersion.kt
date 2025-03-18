/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata

@Serializable
enum class KotlinVersion(
    val versionName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata {
    v1_0(
        versionName = "1.0",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
            stabilizedVersion = KotlinReleaseVersion.v1_0_0,
            deprecatedVersion = KotlinReleaseVersion.v1_3_0,
            removedVersion = KotlinReleaseVersion.v1_4_0,
        )
    ),
    v1_9(
        versionName = "1.9",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_0,
            stabilizedVersion = KotlinReleaseVersion.v1_9_0,
        )
    ),
    v2_0(
        versionName = "2.0",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
            stabilizedVersion = KotlinReleaseVersion.v2_0_0,
        )
    ),
    v2_1(
        versionName = "2.1",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
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
}

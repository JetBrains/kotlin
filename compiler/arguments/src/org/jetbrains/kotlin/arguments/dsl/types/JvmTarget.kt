/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.serialization.json.KotlinJvmTargetAsNameSerializer

@Serializable(with = KotlinJvmTargetAsNameSerializer::class)
enum class JvmTarget(
    val targetName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle
) : WithKotlinReleaseVersionsMetadata {
    jvm1_6(
        targetName = "1.6",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
            deprecatedVersion = KotlinReleaseVersion.v1_9_20,
            removedVersion = KotlinReleaseVersion.v2_0_0
        )
    ),
    jvm1_8(
        targetName = "1.8",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
        )
    );

    companion object {
        const val SUPPORTED_VERSIONS_DESCRIPTION = "1.8 and 9–24"
    }
}

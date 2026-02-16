/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.serialization.json.KotlinJvmDefaultModeAsNameSerializer

@Serializable(with = KotlinJvmDefaultModeAsNameSerializer::class)
enum class JvmDefaultMode(
    val modeName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    ENABLE(
        modeName = "enable",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
            stabilizedVersion = KotlinReleaseVersion.v2_2_0,
        )
    ),
    NO_COMPATIBILITY(
        modeName = "no-compatibility",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
            stabilizedVersion = KotlinReleaseVersion.v2_2_0,
        )
    ),
    DISABLE(
        modeName = "disable",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
            stabilizedVersion = KotlinReleaseVersion.v2_2_0,
        )
    );

    override val stringRepresentation: String
        get() = modeName
}
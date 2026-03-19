/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.serialization.json.KotlinNullabilityAnnotationModeAsNameSerializer

@Serializable
class NullabilityAnnotationConfig(
    val annotationFqName: String,
    val mode: NullabilityAnnotationMode,
)

@Serializable(with = KotlinNullabilityAnnotationModeAsNameSerializer::class)
enum class NullabilityAnnotationMode(
    val modeName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    IGNORE(
        modeName = "ignore",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30,
            stabilizedVersion = KotlinReleaseVersion.v1_5_30,
        )
    ),
    STRICT(
        modeName = "strict",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30,
            stabilizedVersion = KotlinReleaseVersion.v1_5_30,
        )
    ),
    WARN(
        modeName = "warn",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30,
            stabilizedVersion = KotlinReleaseVersion.v1_5_30,
        )
    );

    override val stringRepresentation: String
        get() = modeName
}
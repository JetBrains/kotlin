/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.serialization.json.KotlinAnnotationDefaultTargetModeAsNameSerializer

@Serializable(with = KotlinAnnotationDefaultTargetModeAsNameSerializer::class)
enum class AnnotationDefaultTargetMode(
    val modeName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    FIRST_ONLY(
        modeName = "first-only",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    ),
    FIRST_ONLY_WARN(
        modeName = "first-only-warn",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    ),
    PARAM_PROPERTY(
        modeName = "param-property",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    );

    override val stringRepresentation: String
        get() = modeName
}

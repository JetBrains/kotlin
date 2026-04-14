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
enum class StringConcatMode(
    val modeName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    @SerialName("indy-with-constants")
    INDY_WITH_CONSTANTS(
        modeName = "indy-with-constants",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
            stabilizedVersion = KotlinReleaseVersion.v1_4_20,
        )
    ),

    @SerialName("indy")
    INDY(
        modeName = "indy",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
            stabilizedVersion = KotlinReleaseVersion.v1_4_20,
        )
    ),

    @SerialName("inline")
    INLINE(
        modeName = "inline",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
            stabilizedVersion = KotlinReleaseVersion.v1_4_20,
        )
    );

    override val stringRepresentation: String
        get() = modeName
}

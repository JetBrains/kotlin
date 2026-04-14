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
enum class SourceMapNamesPolicy(
    val policyName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    @SerialName("no")
    NO(
        policyName = "no",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
            stabilizedVersion = KotlinReleaseVersion.v1_8_20,
        )
    ),

    @SerialName("simple-names")
    SIMPLE_NAMES(
        policyName = "simple-names",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
            stabilizedVersion = KotlinReleaseVersion.v1_8_20,
        )
    ),

    @SerialName("fully-qualified-names")
    FULLY_QUALIFIED_NAMES(
        policyName = "fully-qualified-names",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_20,
            stabilizedVersion = KotlinReleaseVersion.v1_8_20,
        )
    );

    override val stringRepresentation: String
        get() = policyName
}

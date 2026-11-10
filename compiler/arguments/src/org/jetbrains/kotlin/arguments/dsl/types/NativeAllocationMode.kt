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
enum class NativeAllocationMode(
    val modeName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    @SerialName("std")
    STD(
        modeName = "std",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("mimalloc")
    MIMALLOC(
        modeName = "mimalloc",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("custom")
    CUSTOM(
        modeName = "custom",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    );

    override val stringRepresentation: String
        get() = modeName
}

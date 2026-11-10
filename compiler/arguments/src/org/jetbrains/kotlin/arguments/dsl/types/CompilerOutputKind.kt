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
enum class CompilerOutputKind(
    val kindName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    @SerialName("program")
    PROGRAM(
        kindName = "program",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("static")
    STATIC(
        kindName = "static",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("dynamic")
    DYNAMIC(
        kindName = "dynamic",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("framework")
    FRAMEWORK(
        kindName = "framework",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("library")
    LIBRARY(
        kindName = "library",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    ),

    @SerialName("bitcode")
    BITCODE(
        kindName = "bitcode",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_20,
        )
    );

    override val stringRepresentation: String
        get() = kindName
}

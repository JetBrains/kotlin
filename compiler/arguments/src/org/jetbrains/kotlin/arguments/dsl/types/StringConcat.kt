/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.serialization.json.StringConcatAsNameSerializer

@Serializable(with = StringConcatAsNameSerializer::class)
enum class StringConcat(
    val schemeName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata {
    INDY_WITH_CONSTANTS(
        "indy-with-constants",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
        )
    ),
    INDY(
        "indy",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
        )
    ),
    INLINE(
        "inline",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_20,
        )
    )
}

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
enum class ValhallaSupportMode(
    val modeName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    @SerialName("none")
    NONE(
        modeName = "none",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_5_0,
        )
    ),
    @SerialName("primitives")
    PRIMITIVES(
        modeName = "primitives",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_5_0,
        )
    ),
    @SerialName("primitivesAndFullValueClasses")
    PRIMITIVES_AND_FULL_VALUE_CLASSES(
        modeName = "primitivesAndFullValueClasses",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_5_0,
        )
    ),
    @SerialName("allValues")
    ALL_VALUES(
        modeName = "allValues",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_5_0,
        )
    );

    override val stringRepresentation: String
        get() = modeName
}

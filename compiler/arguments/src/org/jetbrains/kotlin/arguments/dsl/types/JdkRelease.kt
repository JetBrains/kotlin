/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.serialization.json.KotlinJdkReleaseAsNameSerializer

@Serializable(with = KotlinJdkReleaseAsNameSerializer::class)
enum class JdkRelease(
    val releaseName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata, WithStringRepresentation {
    JDK_1_6(
        releaseName = "1.6",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
            stabilizedVersion = KotlinReleaseVersion.v2_0_0,
        )
    ),
    JDK_1_7(
        releaseName = "1.7",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
            stabilizedVersion = KotlinReleaseVersion.v2_0_0,
        )
    ),
    JDK_1_8(
        releaseName = "1.8",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_6(
        releaseName = "6",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_7(
        releaseName = "7",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_8(
        releaseName = "8",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_9(
        releaseName = "9",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_10(
        releaseName = "10",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_11(
        releaseName = "11",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_12(
        releaseName = "12",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_13(
        releaseName = "13",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_14(
        releaseName = "14",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_15(
        releaseName = "15",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_16(
        releaseName = "16",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_17(
        releaseName = "17",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_18(
        releaseName = "18",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_19(
        releaseName = "19",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_20(
        releaseName = "20",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_21(
        releaseName = "21",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_22(
        releaseName = "22",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_23(
        releaseName = "23",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_24(
        releaseName = "24",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    ),
    JDK_25(
        releaseName = "25",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
            stabilizedVersion = KotlinReleaseVersion.v1_7_0,
        )
    );

    override val stringRepresentation: String
        get() = releaseName
}

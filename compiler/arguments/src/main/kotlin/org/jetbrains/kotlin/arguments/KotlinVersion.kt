/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("name")
@Serializable
sealed class KotlinVersionDetails(
    val addedInVersion: KotlinReleaseVersion,
    val deprecatedInVersion: KotlinReleaseVersion? = null,
    val removedInVersion: KotlinReleaseVersion? = null,
) {
    @Serializable
    @SerialName("1.0")
    class Kotlin10Details : KotlinVersionDetails(
        addedInVersion = KotlinReleaseVersion.KOTLIN_1_4_0,
        deprecatedInVersion = KotlinReleaseVersion.KOTLIN_1_9_20,
        removedInVersion = KotlinReleaseVersion.KOTLIN_2_0_0,
    )

    @Serializable
    @SerialName("1.9")
    class Kotlin19Details : KotlinVersionDetails(
        addedInVersion = KotlinReleaseVersion.KOTLIN_1_9_20,
    )

    @Serializable
    @SerialName("2.0")
    class Kotlin20Details : KotlinVersionDetails(
        addedInVersion = KotlinReleaseVersion.KOTLIN_2_0_0,
    )

    companion object {
        val allKotlinVersions = listOf(
            Kotlin10Details(),
            Kotlin19Details(),
            Kotlin20Details(),
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("name")
@Serializable
sealed class KotlinVersion(val details: KotlinVersionDetails) {
    @Serializable
    @SerialName("1.0")
    object KOTLIN_1_0 : KotlinVersion(KotlinVersionDetails.Kotlin10Details())

    @Serializable
    @SerialName("1.9")
    object KOTLIN_1_9 : KotlinVersion(KotlinVersionDetails.Kotlin19Details())

    @Serializable
    @SerialName("2.0")
    object KOTLIN_2_0 : KotlinVersion(KotlinVersionDetails.Kotlin20Details())
}

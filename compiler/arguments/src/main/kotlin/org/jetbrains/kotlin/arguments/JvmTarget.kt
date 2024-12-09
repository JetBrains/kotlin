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
sealed class JvmTargetDetails(
    val addedInVersion: KotlinReleaseVersion,
    val deprecatedInVersion: KotlinReleaseVersion? = null,
    val removedInVersion: KotlinReleaseVersion? = null,
) {

    @Serializable
    @SerialName("1.6")
    class JvmTargetDetails16 : JvmTargetDetails(
        addedInVersion = KotlinReleaseVersion.KOTLIN_1_4_0,
        deprecatedInVersion = KotlinReleaseVersion.KOTLIN_1_9_20,
        removedInVersion = KotlinReleaseVersion.KOTLIN_2_0_0,
    )

    @Serializable
    @SerialName("1.8")
    class JvmTargetDetails18 : JvmTargetDetails(
        addedInVersion = KotlinReleaseVersion.KOTLIN_1_4_0,
    )

    companion object {
        val allJvmTargets = listOf(
            JvmTargetDetails16(),
            JvmTargetDetails18(),
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("name")
@Serializable
sealed class JvmTarget(val details: JvmTargetDetails) {
    @Serializable
    @SerialName("1.6")
    object JvmTarget_1_6 : JvmTarget(JvmTargetDetails.JvmTargetDetails16())

    @Serializable
    @SerialName("1.8")
    object JvmTarget_1_8 : JvmTarget(JvmTargetDetails.JvmTargetDetails18())
}
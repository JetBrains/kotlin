/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = KotlinJvmTargetAsNameSerializer::class)
@KeepGeneratedSerializer
data class JvmTarget(
    val name: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle
) : WithKotlinReleaseVersionsMetadata

object JvmTargets {
    val jvm1_6 = JvmTarget(
        name = "1.6",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
            deprecatedVersion = KotlinReleaseVersion.v1_9_20,
            removedVersion = KotlinReleaseVersion.v2_0_0
        )
    )
    val jvm1_8 = JvmTarget(
        name = "1.8",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
        )
    )

    val allJvmTargets = setOf(
        jvm1_6,
        jvm1_8,
    )
}

object KotlinJvmTargetAsNameSerializer : KSerializer<JvmTarget> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "org.jetbrains.kotlin.arguments.JvmTarget",
        kind = PrimitiveKind.STRING,
    )

    override fun serialize(
        encoder: Encoder,
        value: JvmTarget
    ) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): JvmTarget {
        val jvmTargetName = decoder.decodeString()
        return JvmTargets.allJvmTargets.single { jvmTarget -> jvmTarget.name == jvmTargetName }
    }
}

object AllDetailsJvmTargetSerializer : KSerializer<Set<JvmTarget>> {
    private val delegateSerializer: KSerializer<Set<JvmTarget>> = SetSerializer(JvmTarget.generatedSerializer())
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: Set<JvmTarget>,
    ) {
        delegateSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Set<JvmTarget> {
        return delegateSerializer.deserialize(decoder)
    }
}

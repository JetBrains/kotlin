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
@Serializable(with = KotlinVersionAsNameSerializer::class)
@KeepGeneratedSerializer
data class KotlinVersion(
    val name: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata

object KotlinVersions {
    val v1_0 = KotlinVersion(
        name = "1.0",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
            deprecatedVersion = KotlinReleaseVersion.v1_9_20,
            removedVersion = KotlinReleaseVersion.v2_0_0,
        )
    )

    val v1_9 = KotlinVersion(
        name = "1.9",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20,
            stabilizedVersion = KotlinReleaseVersion.v1_9_20,
        )
    )

    val v2_0 = KotlinVersion(
        name = "2.0",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20,
            stabilizedVersion = KotlinReleaseVersion.v2_0_0,
        )
    )

    val allKotlinVersions = setOf(
        v1_0,
        v1_9,
        v2_0,
    )
}

object KotlinVersionAsNameSerializer : KSerializer<KotlinVersion> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "org.jetbrains.kotlin.arguments.KotlinVersion",
        kind = PrimitiveKind.STRING,
    )

    override fun serialize(
        encoder: Encoder,
        value: KotlinVersion
    ) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): KotlinVersion {
        val versionName = decoder.decodeString()
        return KotlinVersions.allKotlinVersions.single { version ->  version.name == versionName }
    }
}

object AllDetailsKotlinVersionSerializer : KSerializer<Set<KotlinVersion>> {
    private val delegateSerializer: KSerializer<Set<KotlinVersion>> = SetSerializer(KotlinVersion.generatedSerializer())
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: Set<KotlinVersion>,
    ) {
        delegateSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Set<KotlinVersion> {
        return delegateSerializer.deserialize(decoder)
    }
}

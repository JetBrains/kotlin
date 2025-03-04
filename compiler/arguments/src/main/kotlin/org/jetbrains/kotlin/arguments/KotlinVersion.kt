/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = KotlinVersionAsNameSerializer::class)
enum class KotlinVersion(
    val versionName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata {
    v1_0(
        versionName = "1.0",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
            deprecatedVersion = KotlinReleaseVersion.v1_9_20,
            removedVersion = KotlinReleaseVersion.v2_0_0,
        )
    ),
    v1_9(
        versionName = "1.9",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20,
            stabilizedVersion = KotlinReleaseVersion.v1_9_20,
        )
    ),
    v2_0(
        versionName = "2.0",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20,
            stabilizedVersion = KotlinReleaseVersion.v2_0_0,
        )
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
        encoder.encodeString(value.versionName)
    }

    override fun deserialize(decoder: Decoder): KotlinVersion {
        val versionName = decoder.decodeString()
        return KotlinVersion.entries.single { version -> version.versionName == versionName }
    }
}

object AllDetailsKotlinVersionSerializer : KSerializer<Set<KotlinVersion>> {
    private val delegateSerializer: KSerializer<Set<KotlinVersion>> = SetSerializer(AllKotlinVersionSerializer)
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

object AllKotlinVersionSerializer : KSerializer<KotlinVersion> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        "org.jetbrains.kotlin.arguments.KotlinVersion",
        KotlinReleaseVersionLifecycle.serializer().descriptor
    ) {
        element<String>("name")
        element<KotlinReleaseVersionLifecycle>("releaseVersionsMetadata")
    }

    override fun serialize(encoder: Encoder, value: KotlinVersion) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.versionName)
            encodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer(), value.releaseVersionsMetadata)
        }
    }

    override fun deserialize(decoder: Decoder): KotlinVersion {
        var versionName = ""
        lateinit var metadata: KotlinReleaseVersionLifecycle
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> versionName = decodeStringElement(descriptor, 0)
                    1 -> metadata = decodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        require(versionName.isNotEmpty())
        return KotlinVersion.entries.single { version -> version.versionName == versionName }
    }
}
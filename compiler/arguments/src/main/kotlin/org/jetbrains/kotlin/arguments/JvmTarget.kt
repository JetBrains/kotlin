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
import org.jetbrains.kotlin.arguments.types.KotlinJvmTargetType

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = KotlinJvmTargetAsNameSerializer::class)
enum class JvmTarget(
    val targetName: String,
    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle
) : WithKotlinReleaseVersionsMetadata {
    jvm1_6(
        targetName = "1.6",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
            deprecatedVersion = KotlinReleaseVersion.v1_9_20,
            removedVersion = KotlinReleaseVersion.v2_0_0
        )
    ),
    jvm1_8(
        targetName = "1.8",
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_4_0,
        )
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
        encoder.encodeString(value.targetName)
    }

    override fun deserialize(decoder: Decoder): JvmTarget {
        val jvmTargetName = decoder.decodeString()
        return JvmTarget.entries.single { jvmTarget -> jvmTarget.targetName == jvmTargetName }
    }
}

object AllDetailsJvmTargetSerializer : KSerializer<Set<JvmTarget>> {
    private val delegateSerializer: KSerializer<Set<JvmTarget>> = SetSerializer(AllJvmTargetSerializer)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.arguments.SetJvmTarget") {
        element<String>("type")
        element<Set<JvmTarget>>("values")
    }

    override fun serialize(
        encoder: Encoder,
        value: Set<JvmTarget>,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, KotlinJvmTargetType::class.qualifiedName!!)
            encodeSerializableElement(descriptor, 1, delegateSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): Set<JvmTarget> {
        var type = ""
        val values = mutableSetOf<JvmTarget>()
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> type = decodeStringElement(descriptor, 0)
                    1 -> values.addAll(decodeSerializableElement(descriptor, 1, delegateSerializer))
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        require(type.isNotEmpty() && values.isNotEmpty())
        return values.toSet()
    }
}

object AllJvmTargetSerializer : KSerializer<JvmTarget> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        "org.jetbrains.kotlin.arguments.JvmTarget",
        KotlinReleaseVersionLifecycle.serializer().descriptor
    ) {
        element<String>("name")
        element<KotlinReleaseVersionLifecycle>("releaseVersionsMetadata")
    }

    override fun serialize(encoder: Encoder, value: JvmTarget) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.targetName)
            encodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer(), value.releaseVersionsMetadata)
        }
    }

    override fun deserialize(decoder: Decoder): JvmTarget {
        var targetName = ""
        lateinit var metadata: KotlinReleaseVersionLifecycle
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> targetName = decodeStringElement(descriptor, 0)
                    1 -> metadata = decodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        require(targetName.isNotEmpty())
        return JvmTarget.entries.single { version -> version.targetName == targetName }
    }
}
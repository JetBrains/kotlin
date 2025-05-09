/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersionLifecycle
import org.jetbrains.kotlin.arguments.dsl.types.JvmTarget
import org.jetbrains.kotlin.arguments.dsl.types.KotlinJvmTargetType
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer

object KotlinJvmTargetAsNameSerializer : NamedTypeSerializer<JvmTarget>(
    serialName = "org.jetbrains.kotlin.arguments.JvmTarget",
    nameAccessor = { it.targetName },
    typeFinder = {
        JvmTarget.entries.single { jvmTarget -> jvmTarget.targetName == it }
    }
)

private object AllJvmTargetSerializer : KSerializer<JvmTarget> {
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

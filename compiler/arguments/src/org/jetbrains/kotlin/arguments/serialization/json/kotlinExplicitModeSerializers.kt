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
import org.jetbrains.kotlin.arguments.dsl.types.ExplicitApiMode
import org.jetbrains.kotlin.arguments.dsl.types.KotlinExplicitApiModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer

object KotlinExplicitApiModeAsModeSerializer : NamedTypeSerializer<ExplicitApiMode>(
    serialName = "org.jetbrains.kotlin.config.ExplicitApiMode",
    nameAccessor = { it.modeName },
    typeFinder = {
        ExplicitApiMode.entries.single { mode -> mode.modeName == it }
    }
)

private object AllExplicitApiModeSerializer : KSerializer<ExplicitApiMode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        "org.jetbrains.kotlin.config.ExplicitApiMode",
        KotlinReleaseVersionLifecycle.serializer().descriptor
    ) {
        element<String>("modeName")
        element<KotlinReleaseVersionLifecycle>("releaseVersionsMetadata")
    }

    override fun serialize(encoder: Encoder, value: ExplicitApiMode) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.modeName)
            encodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer(), value.releaseVersionsMetadata)
        }
    }

    override fun deserialize(decoder: Decoder): ExplicitApiMode {
        var modeName = ""
        lateinit var metadata: KotlinReleaseVersionLifecycle
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> modeName = decodeStringElement(descriptor, 0)
                    1 -> metadata = decodeSerializableElement(descriptor, 1, KotlinReleaseVersionLifecycle.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        require(modeName.isNotEmpty())
        return ExplicitApiMode.entries.single { mode -> mode.modeName == modeName }
    }
}

object AllDetailsExplicitApiModeSerializer : KSerializer<Set<ExplicitApiMode>> {
    private val delegateSerializer: KSerializer<Set<ExplicitApiMode>> = SetSerializer(AllExplicitApiModeSerializer)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.config.SetExplicitApiMode") {
        element<String>("type")
        element<Set<ExplicitApiMode>>("values")
    }

    override fun serialize(
        encoder: Encoder,
        value: Set<ExplicitApiMode>,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, KotlinExplicitApiModeType::class.qualifiedName!!)
            encodeSerializableElement(descriptor, 1, delegateSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): Set<ExplicitApiMode> {
        var type = ""
        val values = mutableSetOf<ExplicitApiMode>()
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

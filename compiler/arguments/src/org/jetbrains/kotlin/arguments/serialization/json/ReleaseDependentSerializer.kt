/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.ReleaseDependent

@OptIn(ExperimentalSerializationApi::class)
class ReleaseDependentSerializer<T>(
    private val dataSerializer: KSerializer<T>
) : KSerializer<ReleaseDependent<T>> {

    private val valueInVersionsSerializer = ListSerializer(
        ClosedRangeKotlinReleaseVersionSurrogateSerializer(dataSerializer)
    )

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.arguments.ReleaseDependent") {
        element("current", dataSerializer.descriptor)
        element("valueInVersions", valueInVersionsSerializer.descriptor)
    }

    override fun serialize(
        encoder: Encoder,
        value: ReleaseDependent<T>,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, dataSerializer, value.current)
            encodeSerializableElement(descriptor, 1, valueInVersionsSerializer, value.valueInVersions.toTypedList())
        }
    }

    override fun deserialize(decoder: Decoder): ReleaseDependent<T> {
        var current: T? = null
        val valuesInVersion: MutableMap<ClosedRange<KotlinReleaseVersion>, T> = mutableMapOf()
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> current = decodeSerializableElement(descriptor, index, dataSerializer)
                    1 -> valuesInVersion.putAll(decodeSerializableElement(descriptor, index, valueInVersionsSerializer).fromTypedList())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return ReleaseDependent(
            current = current as T,
            valueInVersions = valuesInVersion.toMap(),
        )
    }
}

@Serializable(with = ClosedRangeKotlinReleaseVersionSurrogateSerializer::class)
private data class ClosedRangeKotlinReleaseVersionSurrogate<T>(
    val minReleaseVersion: KotlinReleaseVersion,
    val maxReleaseVersion: KotlinReleaseVersion,
    val value: T
)

private fun <T> Map<ClosedRange<KotlinReleaseVersion>, T>.toTypedList(): List<ClosedRangeKotlinReleaseVersionSurrogate<T>> = map {
    ClosedRangeKotlinReleaseVersionSurrogate(
        minReleaseVersion = it.key.start,
        maxReleaseVersion = it.key.endInclusive,
        value = it.value
    )
}

private fun <T> List<ClosedRangeKotlinReleaseVersionSurrogate<T>>.fromTypedList(): Map<ClosedRange<KotlinReleaseVersion>, T> = associate {
    it.minReleaseVersion..it.maxReleaseVersion to it.value
}

private class ClosedRangeKotlinReleaseVersionSurrogateSerializer<T>(
    private val dataSerializer: KSerializer<T>
) : KSerializer<ClosedRangeKotlinReleaseVersionSurrogate<T>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.jetbrains.kotlin.arguments.ClosedRangeKotlinReleaseVersion") {
            element("minReleaseVersion", KotlinReleaseVersionAsNameSerializer.descriptor)
            element("maxReleaseVersion", KotlinReleaseVersionAsNameSerializer.descriptor)
            element("value", dataSerializer.descriptor)
        }

    override fun serialize(
        encoder: Encoder,
        value: ClosedRangeKotlinReleaseVersionSurrogate<T>
    ) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, KotlinReleaseVersionAsNameSerializer, value.minReleaseVersion)
            encodeSerializableElement(descriptor, 1, KotlinReleaseVersionAsNameSerializer, value.maxReleaseVersion)
            encodeSerializableElement(descriptor, 2, dataSerializer, value.value)
        }
    }

    override fun deserialize(decoder: Decoder): ClosedRangeKotlinReleaseVersionSurrogate<T> {
        var startVersion: KotlinReleaseVersion? = null
        var endVersion: KotlinReleaseVersion? = null
        var value: T? = null

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> startVersion = decodeSerializableElement(descriptor, index, KotlinReleaseVersionAsNameSerializer)
                    1 -> endVersion = decodeSerializableElement(descriptor, index, KotlinReleaseVersionAsNameSerializer)
                    2 -> value = decodeSerializableElement(descriptor, index, dataSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return ClosedRangeKotlinReleaseVersionSurrogate(
            minReleaseVersion = startVersion ?: error("Failed to deserialize 'minReleaseVersion'"),
            maxReleaseVersion = endVersion ?: error("Failed to deserialize 'maxReleaseVersion'"),
            value = value as T
        )
    }
}
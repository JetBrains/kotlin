/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import org.jetbrains.kotlin.util.GarbageCollectionStats
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.SideStats
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats

object UnitStatsSerializer : KSerializer<UnitStats> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.util.UnitStats") {
        element<String?>("name")
        element<PlatformType>("platform", isOptional = true)
        element<Boolean>("isK2", isOptional = true)
        element<Boolean>("hasErrors", isOptional = true)
        element<Int>("filesCount")
        element<Int>("linesCount")

        element("initStats", TimeSerializer.descriptor)
        element("analysisStats", TimeSerializer.descriptor)
        element("irGenerationStats", TimeSerializer.descriptor)
        element("irLoweringStats", TimeSerializer.descriptor)
        element("backendStats", TimeSerializer.descriptor)

        element("findJavaClassStats", SideStatsSerializer.descriptor, isOptional = true)
        element("findKotlinClassStats", SideStatsSerializer.descriptor, isOptional = true)

        element("gcStats", ListSerializer(GarbageCollectionStatsSerializer).descriptor, isOptional = true)
        element<Long?>("jitTimeMillis", isOptional = true)
        element<List<String>>("extendedStats", isOptional = true)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): UnitStats {
        return decoder.decodeStructure(descriptor) {
            var name: String? = null
            var platform: PlatformType = PlatformType.JVM
            var isK2 = true
            var hasErrors = false
            var filesCount = 0
            var linesCount = 0
            var initStats: Time? = null
            var analysisStats: Time? = null
            var irGenerationStats: Time? = null
            var irLoweringStats: Time? = null
            var backendStats: Time? = null
            var findJavaClassStats: SideStats? = null
            var findKotlinClassStats: SideStats? = null
            var gcStats: List<GarbageCollectionStats> = emptyList()
            var jitTimeMillis: Long? = null
            var extendedStats: List<String> = emptyList()

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> name = decodeStringElement(descriptor, index)
                    1 -> platform = decodeSerializableElement(descriptor, index, PlatformSerializer)
                    2 -> isK2 = decodeBooleanElement(descriptor, index)
                    3 -> hasErrors = decodeBooleanElement(descriptor, index)
                    4 -> filesCount = decodeIntElement(descriptor, index)
                    5 -> linesCount = decodeIntElement(descriptor, index)
                    6 -> initStats = decodeNullableSerializableElement(descriptor, index, TimeSerializer)
                    7 -> analysisStats = decodeNullableSerializableElement(descriptor, index, TimeSerializer)
                    8 -> irGenerationStats = decodeNullableSerializableElement(descriptor, index, TimeSerializer)
                    9 -> irLoweringStats = decodeNullableSerializableElement(descriptor, index, TimeSerializer)
                    10 -> backendStats = decodeNullableSerializableElement(descriptor, index, TimeSerializer)
                    11 -> findJavaClassStats = decodeNullableSerializableElement(descriptor, index, SideStatsSerializer)
                    12 -> findKotlinClassStats =
                        decodeNullableSerializableElement(descriptor, index, SideStatsSerializer)
                    13 -> gcStats =
                        decodeSerializableElement(descriptor, index, ListSerializer(GarbageCollectionStatsSerializer))
                    14 -> jitTimeMillis = decodeNullableSerializableElement(descriptor, index, Long.serializer())
                    15 -> extendedStats = decodeSerializableElement(descriptor, index, ListSerializer(String.serializer()))
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            UnitStats(
                name,
                platform,
                isK2,
                hasErrors,
                filesCount,
                linesCount,
                initStats,
                analysisStats,
                irGenerationStats,
                irLoweringStats,
                backendStats,
                findJavaClassStats,
                findKotlinClassStats,
                gcStats,
                jitTimeMillis,
                extendedStats,
            )
        }
    }

    override fun serialize(encoder: Encoder, value: UnitStats) {
        TODO("Not yet implemented")
    }
}

object PlatformSerializer : KSerializer<PlatformType> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("org.jetbrains.kotlin.util.PlatformType") {
            element<String>("name")
        }

    override fun deserialize(decoder: Decoder): PlatformType {
        return decoder.decodeStructure(descriptor) {
            var name = ""
            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> name = decodeStringElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            PlatformType.valueOf(name)
        }
    }

    override fun serialize(encoder: Encoder, value: PlatformType) {
        TODO("Not yet implemented")
    }
}

object SideStatsSerializer : KSerializer<SideStats> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.util.SideStats") {
        element<Int>("count")
        element("time", TimeSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): SideStats {
        return decoder.decodeStructure(descriptor) {
            var count = 0
            var time = Time.ZERO

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> count = decodeIntElement(descriptor, index)
                    1 -> time = decodeSerializableElement(descriptor, index, TimeSerializer)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            SideStats(count, time)
        }
    }

    override fun serialize(encoder: Encoder, value: SideStats) {
        TODO("Not yet implemented")
    }
}

object TimeSerializer : KSerializer<Time> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.util.Time") {
        element<Long>("nano")
        element<Long>("userNano")
        element<Long>("cpuNano")
    }

    override fun deserialize(decoder: Decoder): Time {
        return decoder.decodeStructure(descriptor) {
            var nano: Long = 0
            var userNano: Long = 0
            var cpuNano: Long = 0

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> nano = decodeLongElement(descriptor, index)
                    1 -> userNano = decodeLongElement(descriptor, index)
                    2 -> cpuNano = decodeLongElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            Time(nano, userNano, cpuNano)
        }
    }

    override fun serialize(encoder: Encoder, value: Time) {
        TODO("Not yet implemented")
    }
}

object GarbageCollectionStatsSerializer : KSerializer<GarbageCollectionStats> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.util.GarbageCollectionStats") {
        element<String>("kind")
        element<Long>("millis")
        element<Long>("count")
    }

    override fun deserialize(decoder: Decoder): GarbageCollectionStats {
        return decoder.decodeStructure(descriptor) {
            var kind = ""
            var millis: Long = 0
            var count: Long = 0

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> kind = decodeStringElement(descriptor, index)
                    1 -> millis = decodeLongElement(descriptor, index)
                    2 -> count = decodeLongElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            GarbageCollectionStats(kind, millis, count)
        }
    }

    override fun serialize(encoder: Encoder, value: GarbageCollectionStats) {
        TODO("Not yet implemented")
    }
}
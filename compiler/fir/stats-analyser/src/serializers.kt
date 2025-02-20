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
import org.jetbrains.kotlin.util.AnalysisStats
import org.jetbrains.kotlin.util.BinaryStats
import org.jetbrains.kotlin.util.GarbageCollectionStats
import org.jetbrains.kotlin.util.InitStats
import org.jetbrains.kotlin.util.IrStats
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats

object UnitStatsSerializer : KSerializer<UnitStats> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.util.UnitStats") {
        element<String?>("name")
        element<PlatformType>("platform", isOptional = true)
        element<Boolean>("isK2", isOptional = true)
        element<Boolean>("hasErrors", isOptional = true)

        element("initStats", TimeSerializer.descriptor)
        element("analysisStats", TimeSerializer.descriptor)
        element("irGenerationStats", TimeSerializer.descriptor)
        element("irLoweringStats", TimeSerializer.descriptor)
        element("backendStats", TimeSerializer.descriptor)

        element("findJavaClassStats", BinaryStatsSerializer.descriptor, isOptional = true)
        element("findKotlinClassStats", BinaryStatsSerializer.descriptor, isOptional = true)

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
            var initStats: InitStats? = null
            var analysisStats: AnalysisStats? = null
            var irGenerationStats: IrStats? = null
            var irLoweringStats: IrStats? = null
            var backendStats: BinaryStats? = null
            var findJavaClassStats: BinaryStats? = null
            var findKotlinClassStats: BinaryStats? = null
            var gcStats: List<GarbageCollectionStats> = emptyList()
            var jitTimeMillis: Long? = null
            var extendedStats: List<String> = emptyList()

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> name = decodeStringElement(descriptor, index)
                    1 -> platform = decodeSerializableElement(descriptor, index, PlatformSerializer)
                    2 -> isK2 = decodeBooleanElement(descriptor, index)
                    3 -> hasErrors = decodeBooleanElement(descriptor, index)
                    4 -> initStats = decodeNullableSerializableElement(descriptor, index, InitStatsSerializer)
                    5 -> analysisStats = decodeNullableSerializableElement(descriptor, index, AnalysisStatsSerializer)
                    6 -> irGenerationStats = decodeNullableSerializableElement(descriptor, index, IrStatsSerializer)
                    7 -> irLoweringStats = decodeNullableSerializableElement(descriptor, index, IrStatsSerializer)
                    8 -> backendStats = decodeNullableSerializableElement(descriptor, index, BinaryStatsSerializer)
                    9 -> findJavaClassStats = decodeNullableSerializableElement(descriptor, index, BinaryStatsSerializer)
                    10 -> findKotlinClassStats =
                        decodeNullableSerializableElement(descriptor, index, BinaryStatsSerializer)
                    11 -> gcStats =
                        decodeSerializableElement(descriptor, index, ListSerializer(GarbageCollectionStatsSerializer))
                    12 -> jitTimeMillis = decodeNullableSerializableElement(descriptor, index, Long.serializer())
                    13 -> extendedStats = decodeSerializableElement(descriptor, index, ListSerializer(String.serializer()))
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            UnitStats(
                name,
                platform,
                isK2,
                hasErrors,
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

object InitStatsSerializer : KSerializer<InitStats> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.util.InitStats") {
        element("time", TimeSerializer.descriptor)
        element<Int>("filesCount")
        element<Int>("linesCount")
    }

    override fun deserialize(decoder: Decoder): InitStats {
        return decoder.decodeStructure(descriptor) {
            var time: Time = Time.ZERO
            var filesCount = 0
            var linesCount = 0

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> time = decodeSerializableElement(BinaryStatsSerializer.descriptor, index, TimeSerializer)
                    1 -> filesCount = decodeIntElement(descriptor, index)
                    2 -> linesCount = decodeIntElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }

            InitStats(time, filesCount, linesCount)
        }
    }

    override fun serialize(encoder: Encoder, value: InitStats) {
        TODO("Not yet implemented")
    }
}

object AnalysisStatsSerializer : KSerializer<AnalysisStats> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.util.AnalysisStats") {
        element("time", TimeSerializer.descriptor)
        element<Int>("allNodesCount")
        element<Int>("leafNodesCount")
        element<Int>("starImportsCount")
    }

    override fun deserialize(decoder: Decoder): AnalysisStats {
        return decoder.decodeStructure(descriptor) {
            var time: Time = Time.ZERO
            var allNodesCount = 0
            var leafNodesCount = 0
            var starImportsCount = 0

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> time = decodeSerializableElement(BinaryStatsSerializer.descriptor, index, TimeSerializer)
                    1 -> allNodesCount = decodeIntElement(descriptor, index)
                    2 -> leafNodesCount = decodeIntElement(descriptor, index)
                    3 -> starImportsCount = decodeIntElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }

            AnalysisStats(time, allNodesCount, leafNodesCount, starImportsCount)
        }
    }

    override fun serialize(encoder: Encoder, value: AnalysisStats) {
        TODO("Not yet implemented")
    }
}

object IrStatsSerializer : KSerializer<IrStats> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.util.IrStats") {
        element("time", TimeSerializer.descriptor)
        element<Int>("allNodesAfterCount")
        element<Int>("leafNodesAfterCount")
    }

    override fun deserialize(decoder: Decoder): IrStats {
        return decoder.decodeStructure(descriptor) {
            var time: Time = Time.ZERO
            var allNodesAfterCount = 0
            var leafNodesAfterCount = 0

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> time = decodeSerializableElement(BinaryStatsSerializer.descriptor, index, TimeSerializer)
                    1 -> allNodesAfterCount = decodeIntElement(descriptor, index)
                    2 -> leafNodesAfterCount = decodeIntElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }

            IrStats(time, allNodesAfterCount, leafNodesAfterCount)
        }
    }

    override fun serialize(encoder: Encoder, value: IrStats) {
        TODO("Not yet implemented")
    }
}

object BinaryStatsSerializer : KSerializer<BinaryStats> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.jetbrains.kotlin.util.SideStats") {
        element("time", TimeSerializer.descriptor)
        element<Int>("count")
        element<Long>("bytesCount")
    }

    override fun deserialize(decoder: Decoder): BinaryStats {
        return decoder.decodeStructure(descriptor) {
            var time = Time.ZERO
            var count = 0
            var bytesCount = 0L

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> time = decodeSerializableElement(descriptor, index, TimeSerializer)
                    1 -> count = decodeIntElement(descriptor, index)
                    2 -> bytesCount = decodeLongElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            BinaryStats(time, count, bytesCount)
        }
    }

    override fun serialize(encoder: Encoder, value: BinaryStats) {
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
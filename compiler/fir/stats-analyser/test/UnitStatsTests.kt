/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.stats.toJson
import org.jetbrains.kotlin.util.AnalysisStats
import org.jetbrains.kotlin.util.BinaryStats
import org.jetbrains.kotlin.util.GarbageCollectionStats
import org.jetbrains.kotlin.util.InitStats
import org.jetbrains.kotlin.util.IrStats
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitStatsTests {
    val exampleUnitStats = UnitStats(
        name = "example",
        platform = PlatformType.JVM,
        isK2 = true,
        hasErrors = false,

        initStats = InitStats(
            Time(1_000_000L, 1_000_001L, 1_000_002L),
            filesCount = 1,
            linesCount = 2
        ),
        analysisStats = AnalysisStats(
            Time(2_000_000L, 2_000_001L, 2_000_002L),
            allNodesCount = 3,
            leafNodesCount = 4,
            starImportsCount = 5,
        ),
        irGenerationStats = IrStats(
            Time(3_000_000L, 3_000_001L, 3_000_002L),
            allNodesAfterCount = 6,
            leafNodesAfterCount = 7,
        ),
        irLoweringStats = IrStats(
            Time(4_000_000L, 4_000_001L, 4_000_002L),
            allNodesAfterCount = 8,
            leafNodesAfterCount = 9,
        ),
        backendStats = BinaryStats(
            Time(5_000_000L, 5_000_001L, 5_000_002L),
            10,
            11
        ),
        findJavaClassStats = BinaryStats(
            Time(6_000_000L, 6_000_001L, 6_000_002L),
            12,
            13
        ),
        findKotlinClassStats = BinaryStats(
            Time(7_000_000L, 7_000_001L, 7_000_002L),
            14,
            15
        ),

        gcStats = listOf(
            GarbageCollectionStats("GC", 8_000L, 16)
        ),

        jitTimeMillis = 9_000L
    )

    val exampleUnitStats2 = UnitStats(
        name = "example2",
        platform = PlatformType.JVM,
        isK2 = true,
        hasErrors = false,
        initStats = InitStats(
            Time(2_000_000L, 2_000_001L, 2_000_002L),
            filesCount = 10,
            linesCount = 200
        ),
        analysisStats = AnalysisStats(
            Time(3_000_000L, 3_000_001L, 3_000_002L),
            allNodesCount = 400,
            leafNodesCount = 600,
            starImportsCount = 26,
        ),
        irGenerationStats = IrStats(
            Time(4_000_000L, 4_000_001L, 4_000_002L),
            allNodesAfterCount = 402,
            leafNodesAfterCount = 602,
        ),
        irLoweringStats = IrStats(
            Time(6_000_000L, 6_000_001L, 6_000_002L),
            allNodesAfterCount = 404,
            leafNodesAfterCount = 604,
        ),
        backendStats = BinaryStats(
            Time(7_000_000L, 7_000_001L, 7_000_002L),
            64,
            2468
        ),
        findJavaClassStats = BinaryStats(
            Time(7_000_000L, 7_000_001L, 7_000_002L),
            4,
            108
        ),
        findKotlinClassStats = BinaryStats(
            Time(8_000_000L, 8_000_001L, 8_000_002L),
            12,
            180
        ),

        gcStats = listOf(
            GarbageCollectionStats("GC", 9_000L, 8),
            GarbageCollectionStats("GC2", 9_000L, 8)
        ),

        jitTimeMillis = 10_000L
    )

    @Test
    fun testJsonSerialization() {
        val json = exampleUnitStats.toJson()
        val deserializedUnitStats = Json.Default.decodeFromString(UnitStatsSerializer, json)

        assertEquals(exampleUnitStats, deserializedUnitStats)
    }

    @Test
    fun testPlusWithEmpty() {
        val resultUnitStats = UnitStats.EMPTY + exampleUnitStats
        assertEquals(exampleUnitStats, resultUnitStats)
    }
}
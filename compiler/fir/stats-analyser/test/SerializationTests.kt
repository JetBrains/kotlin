/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.stats.toJson
import org.jetbrains.kotlin.util.GarbageCollectionStats
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.SideStats
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTests {
    val exampleUnitStats = UnitStats(
        name = "example",
        platform = PlatformType.JVM,
        isK2 = true,
        hasErrors = false,
        filesCount = 5,
        linesCount = 100,

        initStats = Time(1_000_000L, 1_000_001L, 1_000_002L),
        analysisStats = Time(2_000_000L, 2_000_001L, 2_000_002L),
        irGenerationStats = Time(3_000_000L, 3_000_001L, 3_000_002L),
        irLoweringStats = Time(4_000_000L, 4_000_001L, 4_000_002L),
        backendStats = Time(5_000_000L, 5_000_001L, 5_000_002L),

        findJavaClassStats = SideStats(2, Time(6_000_000L, 6_000_001L, 6_000_002L)),
        findKotlinClassStats = SideStats(6, Time(7_000_000L, 7_000_001L, 7_000_002L)),

        gcStats = listOf(
            GarbageCollectionStats("GC", 8_000L, 4)
        ),

        jitTimeMillis = 9_000L
    )

    @Test
    fun testJson() {
        val json = exampleUnitStats.toJson()
        val deserializedUnitStats = Json.Default.decodeFromString(UnitStatsSerializer, json)

        assertEquals(exampleUnitStats, deserializedUnitStats)
    }
}
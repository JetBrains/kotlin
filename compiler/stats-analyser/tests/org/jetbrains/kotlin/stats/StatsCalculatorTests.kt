/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.stats

import org.jetbrains.kotlin.util.CompilerType
import org.jetbrains.kotlin.util.GarbageCollectionStats
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.SideStats
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsCalculatorTests {
    val moduleStats0 = createDummyModuleStats(0, CompilerType.K1, hasErrors = false)
    val moduleStats1 = createDummyModuleStats(1, CompilerType.K2, hasErrors = false)
    val moduleStats2 = createDummyModuleStats(2, CompilerType.K2, hasErrors = true)

    val moduleStats: ModulesReportsData = ModulesReportsData(listOf(moduleStats0, moduleStats1, moduleStats2).associateBy { it.name!! })
    val statsCalculator = StatsCalculator(moduleStats)

    @Test
    fun aggregatedStats() {
        with (statsCalculator.totalStats) {
            assertEquals("Aggregate", name)
            assertEquals(maxOf(moduleStats0.timeStampMs, moduleStats1.timeStampMs, moduleStats2.timeStampMs), timeStampMs)
            assertEquals(PlatformType.JVM, platform)
            assertEquals(CompilerType.K1andK2, compilerType)
            assertTrue(hasErrors)
            assertEquals(moduleStats0.filesCount + moduleStats1.filesCount + moduleStats2.filesCount, filesCount)
            assertEquals(moduleStats0.linesCount + moduleStats1.linesCount + moduleStats2.linesCount, linesCount)
            assertEquals(moduleStats0.initStats!! + moduleStats1.initStats + moduleStats2.initStats, initStats)
            assertEquals(moduleStats0.analysisStats!! + moduleStats1.analysisStats + moduleStats2.analysisStats, analysisStats)
            assertEquals(moduleStats0.translationToIrStats!! + moduleStats1.translationToIrStats + moduleStats2.translationToIrStats, translationToIrStats)
            assertEquals(moduleStats0.irPreLoweringStats!! + moduleStats1.irPreLoweringStats + moduleStats2.irPreLoweringStats, irPreLoweringStats)
            assertEquals(moduleStats0.irSerializationStats!! + moduleStats1.irSerializationStats + moduleStats2.irSerializationStats, irSerializationStats)
            assertEquals(moduleStats0.klibWritingStats!! + moduleStats1.klibWritingStats + moduleStats2.klibWritingStats, klibWritingStats)
            assertEquals(moduleStats0.irLoweringStats!! + moduleStats1.irLoweringStats + moduleStats2.irLoweringStats, irLoweringStats)
            assertEquals(moduleStats0.backendStats!! + moduleStats1.backendStats + moduleStats2.backendStats, backendStats)
            assertEquals(moduleStats0.findJavaClassStats!! + moduleStats1.findJavaClassStats + moduleStats2.findJavaClassStats, findJavaClassStats)
            assertEquals(moduleStats0.findKotlinClassStats!! + moduleStats1.findKotlinClassStats + moduleStats2.findKotlinClassStats, findKotlinClassStats)
            assertEquals(
                listOf(
                    GarbageCollectionStats("0", 0, 0),
                    GarbageCollectionStats("1", 1, 1),
                    GarbageCollectionStats("2", 2, 2),
                ),
                gcStats
            )
            assertEquals(moduleStats0.jitTimeMillis!! + moduleStats1.jitTimeMillis!! + moduleStats2.jitTimeMillis!!, jitTimeMillis)
        }
    }

    @Test
    fun getMaxModulesBy() {
        assertEquals(
            moduleStats2,
            statsCalculator.getTopModulesBy { it.getTotalTime().nanos }.single()
        )
        assertEquals(
            listOf(moduleStats2, moduleStats1),
            statsCalculator.getTopModulesBy(count = 2) { it.getTotalTime().nanos }
        )
    }

    @Test
    fun getMinModulesBy() {
        assertEquals(
            moduleStats0,
            statsCalculator.getTopModulesBy(max = false) { it.getTotalTime().nanos }.single()
        )
        assertEquals(
            listOf(moduleStats0, moduleStats1),
            statsCalculator.getTopModulesBy(max = false, count = 2) { it.getTotalTime().nanos }
        )
    }

    private fun createDummyModuleStats(increment: Int, compilerType: CompilerType, hasErrors: Boolean): UnitStats {
        val time = Time(System.nanoTime() + increment, increment.toLong(), increment.toLong())
        val sideStats = SideStats(increment, time)
        return UnitStats(
            name = increment.toString(),
            timeStampMs = System.currentTimeMillis() + increment,
            platform = PlatformType.JVM,
            compilerType = compilerType,
            hasErrors = hasErrors,
            filesCount = increment,
            linesCount = increment,
            initStats = time,
            analysisStats = time,
            translationToIrStats = time,
            irPreLoweringStats = time,
            irSerializationStats = time,
            klibWritingStats = time,
            irLoweringStats = time,
            backendStats = time,
            findJavaClassStats = sideStats,
            findKotlinClassStats = sideStats,
            gcStats = listOf(
                GarbageCollectionStats(increment.toString(), increment.toLong(), increment.toLong()),
            ),
            jitTimeMillis = increment.toLong(),
        )
    }
}
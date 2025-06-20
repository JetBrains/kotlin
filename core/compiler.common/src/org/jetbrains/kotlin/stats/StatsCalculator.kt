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
import java.util.PriorityQueue

class StatsCalculator(val reportsData: ReportsData) {
    val unitStats = reportsData.unitStats
    val totalStats: UnitStats
    val averageStats: UnitStats

    init {
        unitStats.aggregateStats().let {
            totalStats = it.total
            averageStats = it.average
        }
    }

    /**
     * Use when you are interested in top [count] slowest/fastest modules by the given [selector], where [count] is not very big.
     *
     * If [count] is quite big and [selector] is not very complicated,
     * it's probably worth considering ordinary ascending/descending sorting.
     */
    fun <R : Comparable<R>> getTopModulesBy(
        count: Int = 1,
        max: Boolean = true,
        selector: (UnitStats) -> R
    ): List<UnitStats> {
        if (count == 0) return emptyList()

        // Use the priority queue to avoid accidental O(N) * Log(N) complexity due to the sorting of the full collection.
        // If the count is small (typically a reasonable number is lower than 10),
        // the algorithm has O(N) * Log(count) complexity that makes it faster than full sorting.
        val priorityQueue = PriorityQueue<UnitStats>(count, Comparator { o1, o2 ->
            val value1 = selector(o1)
            val value2 = selector(o2)
            if (max) {
                value1.compareTo(value2)
            } else {
                value2.compareTo(value1)
            }
        })

        var bottomValue: R? = null
        for (unitStat in unitStats) {
            if (priorityQueue.size < count) {
                priorityQueue.add(unitStat)
                bottomValue = selector(priorityQueue.peek())
            } else {
                val calculatedSelector = selector(unitStat)
                val replace = if (max) {
                    calculatedSelector > bottomValue!!
                } else {
                    calculatedSelector < bottomValue!!
                }

                if (replace) {
                    priorityQueue.poll()
                    priorityQueue.add(unitStat)
                    bottomValue = selector(priorityQueue.peek())
                }
            }
        }

        return if (max)
            priorityQueue.sortedByDescending(selector)
        else
            priorityQueue.sortedBy(selector)
    }

    data class AggregatedStats(val total: UnitStats, val average: UnitStats)

    private fun Collection<UnitStats>.aggregateStats(): AggregatedStats {
        require(isNotEmpty()) { "At least one entry is required" }

        if (size == 1) return first().let { AggregatedStats(it, it) }

        var name: String? = null
        var outputKind: String? = null
        var latestCurrentTimeMs: Long? = null
        var platform: PlatformType? = null
        var compilerType: CompilerType? = null
        var hasErrors = false
        var filesCount = 0L
        var linesCount = 0L
        var initStats = Time.ZERO
        var analysisStats = Time.ZERO
        var translationToIrStats: Time = Time.ZERO
        var irPreLoweringStats: Time = Time.ZERO
        var irSerializationStats: Time = Time.ZERO
        var klibWritingStats: Time = Time.ZERO
        var irLoweringStats: Time = Time.ZERO
        var backendStats: Time = Time.ZERO
        var findJavaClassStats: SideStats = SideStats.EMPTY
        var findKotlinClassStats: SideStats = SideStats.EMPTY
        val gcStats = mutableMapOf<String, Pair<GarbageCollectionStats, Long>>()
        var jitTimeMillis: Long = 0

        for (moduleStats in this) {
            if (name == null) {
                name = moduleStats.name
            } else if (name != moduleStats.name) {
                name = "Aggregate"
            }
            if (outputKind == null) {
                outputKind = moduleStats.name
            } else if (outputKind != moduleStats.outputKind) {
                name = "Aggregate"
            }
            if (latestCurrentTimeMs == null || latestCurrentTimeMs < moduleStats.timeStampMs) {
                latestCurrentTimeMs = moduleStats.timeStampMs
            }
            if (platform == null) {
                platform = moduleStats.platform
            } else if (platform != moduleStats.platform) {
                println("The module ${moduleStats.name} is ignored because it has different platform ${moduleStats.platform} (not $platform")
            }
            compilerType = moduleStats.compilerType + compilerType
            hasErrors = hasErrors || moduleStats.hasErrors
            filesCount += moduleStats.filesCount
            linesCount += moduleStats.linesCount
            initStats += moduleStats.initStats
            analysisStats += moduleStats.analysisStats
            translationToIrStats += moduleStats.translationToIrStats
            irPreLoweringStats += moduleStats.irPreLoweringStats
            irSerializationStats += moduleStats.irSerializationStats
            klibWritingStats += moduleStats.klibWritingStats
            irLoweringStats += moduleStats.irLoweringStats
            backendStats += moduleStats.backendStats
            findJavaClassStats += moduleStats.findJavaClassStats
            findKotlinClassStats += moduleStats.findKotlinClassStats
            for (gcInfo in moduleStats.gcStats) {
                val gcKind = gcInfo.kind
                val (existingGcStats, count) = gcStats.getOrPut(gcKind) { GarbageCollectionStats(gcKind, 0L, 0L) to 0L }
                gcStats[gcKind] =
                    GarbageCollectionStats(
                        gcKind,
                        existingGcStats.millis + gcInfo.millis,
                        existingGcStats.count + gcInfo.count
                    ) to count + 1
            }
            jitTimeMillis += moduleStats.jitTimeMillis ?: 0
        }

        fun getStats(total: Boolean): UnitStats {
            return UnitStats(
                name = name,
                outputKind = outputKind,
                timeStampMs = latestCurrentTimeMs ?: System.currentTimeMillis(),
                platform = platform!!,
                compilerType = compilerType ?: CompilerType.K1andK2,
                hasErrors = hasErrors,
                filesCount = filesCount.let { if (total) it else it / size }.toInt(),
                linesCount = linesCount.let { if (total) it else it / size }.toInt(),
                initStats = initStats.let { if (total) it else it / size },
                analysisStats = analysisStats.let { if (total) it else it / size },
                translationToIrStats = translationToIrStats.let { if (total) it else it / size },
                irPreLoweringStats = irPreLoweringStats.let { if (total) it else it / size },
                irSerializationStats = irSerializationStats.let { if (total) it else it / size },
                klibWritingStats = klibWritingStats.let { if (total) it else it / size },
                irLoweringStats = irLoweringStats.let { if (total) it else it / size },
                backendStats = backendStats.let { if (total) it else it / size },
                findJavaClassStats = findJavaClassStats.let { if (total) it else it / size },
                findKotlinClassStats = findKotlinClassStats.let { if (total) it else it / size },
                gcStats = gcStats.values.map { gcStatsToCount ->
                    val (gcStats, count) = gcStatsToCount
                    GarbageCollectionStats(
                        gcStats.kind,
                        gcStats.millis.let { if (total) it else it / count },
                        gcStats.count.let { if (total) it else it / count }
                    )
                },
                jitTimeMillis = jitTimeMillis.let { if (total) it else it / size },
            )
        }

        return AggregatedStats(getStats(total = true), getStats(total = false))
    }
}


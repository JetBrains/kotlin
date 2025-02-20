/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import java.util.concurrent.TimeUnit

class ModulesStats(
    val modulesStats: List<UnitStats>,
) {
    val totalStats: UnitStats by lazy {
        modulesStats.fold(UnitStats.EMPTY) { result, moduleStat -> result + moduleStat }
    }
}

fun UnitStats.totalTime(): Time {
    return Time.ZERO + initStats?.time + analysisStats?.time + irGenerationStats?.time + irLoweringStats?.time + backendStats?.time + findKotlinClassStats?.time + findJavaClassStats?.time
}

fun UnitStats.lps(): Double = entitiesPerSecond(entitySelector = { it.initStats!!.linesCount.toLong() }) { it.totalTime().nano }

fun UnitStats.entitiesPerSecond(entitySelector: (UnitStats) -> Long, timeSelectorNano: (UnitStats) -> Long): Double {
    return entitySelector(this).toDouble() * TimeUnit.SECONDS.toNanos(1) / timeSelectorNano(this)
}

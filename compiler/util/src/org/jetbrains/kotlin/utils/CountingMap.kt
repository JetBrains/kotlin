/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

/**
 * [CountingMap] wraps any [MutableMap] and collects the number of entries left in the map upon finalization.
 * [printStatistics] can be called to print statistics on those usage numbers.
 *
 * This class is not meant to be used in production, but rather as a helper when investigating performance issues around
 * [Object.hashCode] to see if replacing the [MutableMap] with [SmartIdentityTable] would make sense.

 * The implementation of [CountingMap] is not synchronized, unless the [container] is.
 */
class CountingMap<K, V>(val tag: String, val container: MutableMap<K, V>) : MutableMap<K, V> by container {

    @Suppress("ProtectedInFinal", "Unused")
    protected fun finalize() {
        var list = tags[tag]
        if (list == null) {
            list = mutableListOf()
            tags[tag] = list
        }
        list.add(size)
    }

    companion object {
        val tags = HashMap<String, MutableList<Int>>()

        fun printStatistics() {
            println(
                """
                Map statistics (by tag)
                =======================
                """.trimIndent()
            )

            for (tag in tags) {

                var name = tag.key
                var entries = tag.value

                var total = 0
                var min = Int.MAX_VALUE
                var max = Int.MIN_VALUE

                for (entry in entries) {
                    total += entry
                    min = Math.min(min, entry)
                    max = Math.max(max, entry)
                }

                val mean = total / entries.size

                var squaredDiffTotalFromMean = 0.0
                for (entry in entries) {
                    squaredDiffTotalFromMean += Math.pow(entry.toDouble() - mean, 2.0)
                }
                val stddev = Math.sqrt(squaredDiffTotalFromMean / entries.size)

                entries.sort()
                val median = entries[entries.size / 2]
                val p90 = entries[(entries.size * 0.9).toInt()]
                val p95 = entries[(entries.size * 0.95).toInt()]

                println(
                    """
                    $name:
                    ${"-".repeat(name.length + 1)}
                    instances: $total
                    min: $min
                    max: $max
                    mean: $mean
                    stddev: $stddev
                    median: $median
                    90p: $p90
                    95p: $p95
                    """.trimIndent()
                )
            }
        }
    }
}

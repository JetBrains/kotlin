// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.resolve

import com.intellij.debugger.streams.resolve.ValuesOrderResolver
import com.intellij.debugger.streams.trace.TraceElement
import com.intellij.debugger.streams.trace.TraceInfo

class WindowedResolver : ValuesOrderResolver {
    override fun resolve(info: TraceInfo): ValuesOrderResolver.Result {
        val indexBefore = info.valuesOrderBefore
        val indexAfter = info.valuesOrderAfter

        val timesBefore = indexBefore.keys.sorted().toIntArray()
        val timesAfter = indexAfter.keys.sorted().toIntArray()

        if (timesAfter.isEmpty()) return emptyTransitions(indexBefore)

        val direct = mutableMapOf<TraceElement, MutableList<TraceElement>>()
        val reverse = mutableMapOf<TraceElement, List<TraceElement>>()

        var windowStartIndex = 0
        var windowEndIndex = calcWindowSize(timesBefore, timesAfter)
        for (timeAfter in timesAfter) {
            if (windowEndIndex == timesAfter.size) {
                windowStartIndex += 1
            } else {
                while (windowEndIndex < timesBefore.size && timesBefore[windowEndIndex] < timeAfter) {
                    windowStartIndex += 1
                    windowEndIndex += 1
                }
            }

            val window = (windowStartIndex until windowEndIndex).asSequence()
                .map { indexBefore[timesBefore[it]]!! }
                .toList()
            val mappedElement = indexAfter[timeAfter]!!
            window.forEach { direct.computeIfAbsent(it, { mutableListOf() }).add(mappedElement) }
            reverse[mappedElement] = window
        }

        return ValuesOrderResolver.Result.of(direct, reverse)
    }

    private fun calcWindowSize(before: IntArray, after: IntArray): Int {
        var size = 0
        while (size < before.size && before[size] < after[0]) size += 1
        return size
    }

    private fun emptyTransitions(indexBefore: MutableMap<Int, TraceElement>): ValuesOrderResolver.Result {
        val direct = indexBefore.asSequence().sortedBy { it.key }.associate { it.value to emptyList<TraceElement>() }
        return ValuesOrderResolver.Result.of(direct, emptyMap())
    }
}
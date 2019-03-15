// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.resolve

import com.intellij.debugger.streams.resolve.ValuesOrderResolver
import com.intellij.debugger.streams.trace.TraceElement
import com.intellij.debugger.streams.trace.TraceInfo

class ChunkedResolver : ValuesOrderResolver {
    override fun resolve(info: TraceInfo): ValuesOrderResolver.Result {
        val beforeIndex = info.valuesOrderBefore
        val afterIndex = info.valuesOrderAfter

        val invertedOrder = mutableMapOf<Int, MutableList<Int>>()
        val beforeTimes = beforeIndex.keys.sorted().toTypedArray()
        val afterTimes = afterIndex.keys.sorted().toTypedArray()

        var beforeIx = 0
        for (afterTime in afterTimes) {
            while (beforeIx < beforeTimes.size && beforeTimes[beforeIx] < afterTime) {
                invertedOrder.computeIfAbsent(afterTime, { _ -> mutableListOf() }).add(beforeTimes[beforeIx])
                beforeIx += 1
            }
        }

        val direct = mutableMapOf<TraceElement, List<TraceElement>>()
        val reverse = mutableMapOf<TraceElement, List<TraceElement>>()
        for ((timeAfter, elementAfter) in afterIndex) {
            val before: List<Int> = invertedOrder[timeAfter] ?: emptyList()
            val beforeElements = before.map { beforeIndex[it]!! }
            beforeElements.forEach { direct[it] = listOf(elementAfter) }
            reverse[elementAfter] = beforeElements
        }

        return ValuesOrderResolver.Result.of(direct, reverse)
    }
}
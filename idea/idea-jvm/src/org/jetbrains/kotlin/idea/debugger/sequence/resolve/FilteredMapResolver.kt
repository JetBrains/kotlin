// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.resolve

import com.intellij.debugger.streams.resolve.ValuesOrderResolver
import com.intellij.debugger.streams.trace.TraceElement
import com.intellij.debugger.streams.trace.TraceInfo

class FilteredMapResolver : ValuesOrderResolver {
    override fun resolve(info: TraceInfo): ValuesOrderResolver.Result {
        val before = info.valuesOrderBefore
        val after = info.valuesOrderAfter

        val invertedOrder = mutableMapOf<Int, Int>()
        val beforeTimes = before.keys.sorted().toIntArray()
        val afterTimes = after.keys.sorted().toIntArray()
        var beforeIndex = 0
        for (afterTime in afterTimes) {
            while (beforeIndex < beforeTimes.size && afterTime > beforeTimes[beforeIndex]) beforeIndex += 1
            val beforeTime = beforeTimes[beforeIndex - 1]
            if (beforeTime < afterTime) {
                invertedOrder[afterTime] = beforeTime
            }
        }

        val direct = mutableMapOf<TraceElement, List<TraceElement>>()
        val reverse = mutableMapOf<TraceElement, List<TraceElement>>()

        for ((afterTime, beforeTime) in invertedOrder) {
            val beforeElement = before.getValue(beforeTime)
            val afterElement = after.getValue(afterTime)
            direct[beforeElement] = listOf(afterElement)
            reverse[afterElement] = listOf(beforeElement)
        }

        return ValuesOrderResolver.Result.of(direct, reverse)
    }
}
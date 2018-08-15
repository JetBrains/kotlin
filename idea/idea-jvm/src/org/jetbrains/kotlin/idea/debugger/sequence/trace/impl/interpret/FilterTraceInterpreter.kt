// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.interpret

import com.intellij.debugger.streams.trace.CallTraceInterpreter
import com.intellij.debugger.streams.trace.TraceElement
import com.intellij.debugger.streams.trace.TraceInfo
import com.intellij.debugger.streams.trace.impl.TraceElementImpl
import com.intellij.debugger.streams.trace.impl.interpret.ex.UnexpectedValueTypeException
import com.intellij.debugger.streams.wrapper.StreamCall
import com.sun.jdi.ArrayReference
import com.sun.jdi.BooleanValue
import com.sun.jdi.IntegerValue
import com.sun.jdi.Value

class FilterTraceInterpreter(private val predicateValueToAccept: Boolean) : CallTraceInterpreter {
    override fun resolve(call: StreamCall, value: Value): TraceInfo {
        if (value !is ArrayReference) throw UnexpectedValueTypeException("array reference excepted, but actual: ${value.type().name()}")
        val before = resolveValuesBefore(value.getValue(0))
        val filteringMap = value.getValue(1)
        val after = resolveValuesAfter(before, filteringMap)

        return ValuesOrder(call, before, after)
    }

    private fun resolveValuesBefore(map: Value): Map<Int, TraceElement> {
        val (keys, objects) = InterpreterUtil.extractMap(map)
        val result: MutableList<TraceElement> = mutableListOf()
        for (i in 0.until(keys.length())) {
            val time = keys.getValue(i)
            val value = objects.getValue(i)
            if (time !is IntegerValue) throw UnexpectedValueTypeException("time should be represented by integer value")
            result.add(TraceElementImpl(time.value(), value))
        }

        return InterpreterUtil.createIndexByTime(result)
    }

    private fun resolveValuesAfter(before: Map<Int, TraceElement>, filteringMap: Value): Map<Int, TraceElement> {
        val predicateValues = extractPredicateValues(filteringMap)
        val result = linkedMapOf<Int, TraceElement>()
        for ((beforeTime, element) in before) {
            val predicateValue = predicateValues[beforeTime]
            if (predicateValue == predicateValueToAccept) {
                result[beforeTime + 1] = TraceElementImpl(beforeTime + 1, element.value)
            }
        }

        return result
    }

    private fun extractPredicateValues(filteringMap: Value): Map<Int, Boolean> {
        val (keys, values) = InterpreterUtil.extractMap(filteringMap)
        val result = mutableMapOf<Int, Boolean>()
        for (i in 0.until(keys.length())) {
            val time = keys.getValue(i)
            val value = values.getValue(i)
            if (time !is IntegerValue) throw UnexpectedValueTypeException("time should be represented by integer value")
            if (value !is BooleanValue) throw UnexpectedValueTypeException("predicate value should be represented by boolean value")
            result[time.value()] = value.value()
        }

        return result
    }
}
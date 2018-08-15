// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.interpret

import com.intellij.debugger.streams.trace.TraceElement
import com.intellij.debugger.streams.trace.impl.interpret.ex.UnexpectedValueException
import com.sun.jdi.ArrayReference
import com.sun.jdi.Value

object InterpreterUtil {

    fun extractMap(value: Value): MapRepresentation {
        if (value !is ArrayReference || value.length() != 2) {
            throw UnexpectedValueException("Map should be represented by array with two nested arrays: keys and values")
        }

        val keys = value.getValue(0)
        val values = value.getValue(1)

        if (keys !is ArrayReference || values !is ArrayReference || keys.length() != values.length()) {
            throw UnexpectedValueException("Keys and values should be arrays with equal counts of elements")
        }

        return MapRepresentation(keys, values)
    }

    fun createIndexByTime(elements: List<TraceElement>): Map<Int, TraceElement> =
        elements.associate { elem -> elem.time to elem }

    data class MapRepresentation(val keys: ArrayReference, val values: ArrayReference)
}

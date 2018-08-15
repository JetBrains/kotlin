// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.interpret

import com.intellij.debugger.streams.trace.TraceElement
import com.intellij.debugger.streams.trace.TraceInfo
import com.intellij.debugger.streams.wrapper.StreamCall

class ValuesOrder(
    private val call: StreamCall,
    private val before: Map<Int, TraceElement>,
    private val after: Map<Int, TraceElement>
) : TraceInfo {
    override fun getValuesOrderBefore(): Map<Int, TraceElement> = before

    override fun getCall(): StreamCall = call

    override fun getValuesOrderAfter(): Map<Int, TraceElement> = after

    override fun getDirectTrace(): MutableMap<TraceElement, MutableList<TraceElement>>? = null

    override fun getReverseTrace(): MutableMap<TraceElement, MutableList<TraceElement>>? = null
}
package com.intellij.debugger.streams.kotlin.trace.impl.interpret

import com.intellij.debugger.streams.trace.CallTraceInterpreter
import com.intellij.debugger.streams.trace.TraceInfo
import com.intellij.debugger.streams.trace.impl.interpret.ValuesOrderInfo
import com.intellij.debugger.streams.trace.impl.interpret.ex.UnexpectedValueTypeException
import com.intellij.debugger.streams.wrapper.StreamCall
import com.sun.jdi.ArrayReference
import com.sun.jdi.Value

/**
 * @author Vitaliy.Bibaev
 */
class FilterTraceInterpreter : CallTraceInterpreter {
  override fun resolve(call: StreamCall, value: Value): TraceInfo {
    if (value !is ArrayReference) throw UnexpectedValueTypeException("array reference excepted, but actual: ${value.type().name()}")
    val times = value.getValue(0)
    val values = value.getValue(1)
    val predicateResults = value.getValue(0)
    return ValuesOrderInfo.empty(call)
  }
}
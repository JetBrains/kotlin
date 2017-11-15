package com.intellij.debugger.streams.kotlin.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.dsl.*
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.StreamCall
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall

/**
 * @author Vitaliy.Bibaev
 */
interface BothSemanticsHandler {
  fun variablesDeclaration(call: StreamCall, order: Int, dsl: Dsl): List<VariableDeclaration>

  fun prepareResult(dsl: Dsl, variables: List<Variable>): CodeBlock

  fun additionalCallsBefore(call: StreamCall, dsl: Dsl): List<IntermediateStreamCall>

  fun additionalCallsAfter(call: StreamCall, dsl: Dsl): List<IntermediateStreamCall>

  fun getResultExpression(call: StreamCall, variables: List<Variable>): Expression

  fun transformAsIntermediateCall(call: IntermediateStreamCall, dsl: Dsl): IntermediateStreamCall

  fun transformAsTerminalCall(call: TerminatorStreamCall, dsl: Dsl): TerminatorStreamCall
}
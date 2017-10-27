package com.intellij.debugger.streams.kotlin.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.impl.handler.unified.HandlerBase
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall

/**
 * @author Vitaliy.Bibaev
 */
class FilterTerminatorHandler(call: TerminatorStreamCall, resultExpression: String, dsl: Dsl)
  : HandlerBase.Terminal(dsl) {
  override fun additionalCallsBefore(): List<IntermediateStreamCall> = emptyList()

  override fun additionalVariablesDeclaration(): List<VariableDeclaration> {
    return emptyList()
  }

  override fun getResultExpression(): Expression = TextExpression("1")

  override fun prepareResult(): CodeBlock {
    return dsl.block { }
  }
}
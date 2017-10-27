package com.intellij.debugger.streams.kotlin.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.dsl.*
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.impl.handler.unified.HandlerBase
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall

/**
 * @author Vitaliy.Bibaev
 */
class FilterIntermediateHandler(number: Int, call: IntermediateStreamCall, dsl: Dsl) : HandlerBase.Intermediate(dsl) {
  private val itemsMap = dsl.linkedMap(dsl.types.INT, call.typeBefore, "filter${number}ItemsMap")
  override fun additionalCallsAfter(): List<IntermediateStreamCall> {
    return emptyList()
  }

  override fun additionalCallsBefore(): List<IntermediateStreamCall> {
    return emptyList()
  }

  override fun transformCall(call: IntermediateStreamCall): IntermediateStreamCall {
    // TODO: transform it
    return call
  }

  override fun additionalVariablesDeclaration(): List<VariableDeclaration> {
    return listOf(itemsMap.defaultDeclaration())
  }

  override fun getResultExpression(): Expression {
    return dsl.newArray(dsl.types.ANY, TextExpression("values"))
  }

  override fun prepareResult(): CodeBlock {
    return itemsMap.convertToArray(dsl, "values")
  }

}
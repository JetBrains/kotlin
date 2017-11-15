package com.intellij.debugger.streams.kotlin.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.TerminatorCallHandler
import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall

/**
 * @author Vitaliy.Bibaev
 */
class CollectionTerminatorHandler(private val call: TerminatorStreamCall,
                                  private val resultExpression: String,
                                  private val dsl: Dsl,
                                  private val internalHandler: BothSemanticsHandler)
  : TerminatorCallHandler, CollectionHandlerBase(Int.MAX_VALUE, dsl, call, internalHandler) {

  override fun prepareResult(): CodeBlock {
    val prepareResult = internalHandler.prepareResult(dsl, variables)
    val additionalCallsAfter = internalHandler.additionalCallsAfter(call, dsl)
    if (additionalCallsAfter.isEmpty()) {
      return prepareResult
    }

    return dsl.block {
      +createCallAfterExpression(additionalCallsAfter)
      add(prepareResult)
    }
  }

  override fun additionalCallsBefore(): List<IntermediateStreamCall> =
      internalHandler.additionalCallsBefore(call, dsl)

  override fun transformCall(call: TerminatorStreamCall): TerminatorStreamCall {
    return internalHandler.transformAsTerminalCall(call, dsl)
  }

  private fun createCallAfterExpression(additionalCallsAfter: List<IntermediateStreamCall>): Expression {
    var result: Expression = TextExpression(resultExpression)
    for (call in additionalCallsAfter) {
      val args = call.arguments.map { TextExpression(it.text) }.toTypedArray()
      result = result.call(call.name, *args)
    }

    return result
  }
}
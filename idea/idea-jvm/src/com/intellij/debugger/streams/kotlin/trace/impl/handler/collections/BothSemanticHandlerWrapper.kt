package com.intellij.debugger.streams.kotlin.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.IntermediateCallHandler
import com.intellij.debugger.streams.trace.TerminatorCallHandler
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall

/**
 * Unlike java streams, most operations in kotlin collections are intermediate and terminal simultaneously.
 * To avoid using of the same code in two places we will implement common logic in the {@link BothSemanticsHandler}.
 *
 * @author Vitaliy.Bibaev
 */
class BothSemanticHandlerWrapper(private val handler: BothSemanticsHandler) {
  fun createIntermediateHandler(order: Int, call: IntermediateStreamCall, dsl: Dsl): IntermediateCallHandler =
      CollectionIntermediateHandler(order, call, dsl, handler)

  fun createTerminatorHandler(call: TerminatorStreamCall, resultExpression: String, dsl: Dsl): TerminatorCallHandler =
      CollectionTerminatorHandler(call, resultExpression, dsl, handler)
}

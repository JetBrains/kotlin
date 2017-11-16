package com.intellij.debugger.streams.kotlin.lib

import com.intellij.debugger.streams.kotlin.resolve.FilterOrderResolver
import com.intellij.debugger.streams.kotlin.trace.impl.handler.collections.*
import com.intellij.debugger.streams.kotlin.trace.impl.interpret.FilterTraceInterpreter
import com.intellij.debugger.streams.lib.IntermediateOperation
import com.intellij.debugger.streams.lib.TerminalOperation
import com.intellij.debugger.streams.lib.impl.LibrarySupportBase
import com.intellij.debugger.streams.resolve.ValuesOrderResolver
import com.intellij.debugger.streams.trace.CallTraceInterpreter
import com.intellij.debugger.streams.trace.IntermediateCallHandler
import com.intellij.debugger.streams.trace.TerminatorCallHandler
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall

/**
 * @author Vitaliy.Bibaev
 */
class KotlinCollectionLibrarySupport : LibrarySupportBase() {
  init {
    addOperation(FilterOperation("filter", FilterCallHandler()))
  }

  private fun addOperation(operation: CollectionOperation) {
    addIntermediateOperationsSupport(operation)
    addTerminationOperationsSupport(operation)
  }

  private abstract class CollectionOperation(override val name: String,
                                             handler: BothSemanticsHandler)
    : IntermediateOperation, TerminalOperation {

    private val wrapper = BothSemanticHandlerWrapper(handler)

    override fun getTraceHandler(callOrder: Int, call: IntermediateStreamCall, dsl: Dsl): IntermediateCallHandler =
        wrapper.createIntermediateHandler(callOrder, call, dsl)

    override fun getTraceHandler(call: TerminatorStreamCall, resultExpression: String, dsl: Dsl): TerminatorCallHandler =
        wrapper.createTerminatorHandler(call, resultExpression, dsl)
  }

  private class FilterOperation(name: String, handler: BothSemanticsHandler)
    : CollectionOperation(name, handler) {
    override val traceInterpreter: CallTraceInterpreter = FilterTraceInterpreter()
    override val valuesOrderResolver: ValuesOrderResolver = FilterOrderResolver()
  }
}

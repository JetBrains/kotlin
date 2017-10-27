package com.intellij.debugger.streams.kotlin.lib

import com.intellij.debugger.streams.kotlin.resolve.FilterOrderResolver
import com.intellij.debugger.streams.kotlin.trace.impl.handler.collections.FilterIntermediateHandler
import com.intellij.debugger.streams.kotlin.trace.impl.handler.collections.FilterTerminatorHandler
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
    addOperation(FilterOperation("filter",
        { num, call, dsl -> FilterIntermediateHandler(num, call, dsl) },
        { call, resultExpression, dsl -> FilterTerminatorHandler(call, resultExpression, dsl)}))
  }

  private fun addOperation(operation: CollectionOperation) {
    addIntermediateOperationsSupport(operation)
    addTerminationOperationsSupport(operation)
  }

  private abstract class CollectionOperation(override val name: String,
                                             private val intermediateHandlerProvider: IntermediateHandlerProvider,
                                             private val terminatorCallProvider: TerminatorHandlerProvider)
    : IntermediateOperation, TerminalOperation {
    override fun getTraceHandler(callOrder: Int, call: IntermediateStreamCall, dsl: Dsl): IntermediateCallHandler =
        intermediateHandlerProvider(callOrder, call, dsl)

    override fun getTraceHandler(call: TerminatorStreamCall, resultExpression: String, dsl: Dsl): TerminatorCallHandler =
        terminatorCallProvider(call, resultExpression, dsl)
  }

  private class FilterOperation(name: String, intermediateHandlerProvider: IntermediateHandlerProvider,
                                terminatorHandlerProvider: TerminatorHandlerProvider)
    : CollectionOperation(name, intermediateHandlerProvider, terminatorHandlerProvider) {
    override val traceInterpreter: CallTraceInterpreter = FilterTraceInterpreter()
    override val valuesOrderResolver: ValuesOrderResolver = FilterOrderResolver()
  }
}

private typealias IntermediateHandlerProvider = (Int, IntermediateStreamCall, Dsl) -> IntermediateCallHandler
private typealias TerminatorHandlerProvider = (TerminatorStreamCall, String, Dsl) -> TerminatorCallHandler

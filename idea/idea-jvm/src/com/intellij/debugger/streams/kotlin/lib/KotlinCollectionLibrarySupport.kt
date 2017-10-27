package com.intellij.debugger.streams.kotlin.lib

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
    addIntermediateOperationsSupport()
    addTerminationOperationsSupport()
  }

  private fun addOperation(operation: CollectionOperation) {
    addIntermediateOperationsSupport(operation)
    addTerminationOperationsSupport(operation)
  }

  private class CollectionOperation : IntermediateOperation, TerminalOperation {
    override val name: String
      get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val traceInterpreter: CallTraceInterpreter
      get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val valuesOrderResolver: ValuesOrderResolver
      get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun getTraceHandler(callOrder: Int, call: IntermediateStreamCall, dsl: Dsl): IntermediateCallHandler {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTraceHandler(call: TerminatorStreamCall, resultExpression: String, dsl: Dsl): TerminatorCallHandler {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
  }
}
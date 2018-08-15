// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.lib.collections

import com.intellij.debugger.streams.lib.IntermediateOperation
import com.intellij.debugger.streams.lib.TerminalOperation
import com.intellij.debugger.streams.lib.impl.LibrarySupportBase
import com.intellij.debugger.streams.resolve.FilterResolver
import com.intellij.debugger.streams.resolve.ValuesOrderResolver
import com.intellij.debugger.streams.trace.CallTraceInterpreter
import com.intellij.debugger.streams.trace.IntermediateCallHandler
import com.intellij.debugger.streams.trace.TerminatorCallHandler
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.collections.BothSemanticHandlerWrapper
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.collections.BothSemanticsHandler
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.collections.FilterCallHandler
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.interpret.FilterTraceInterpreter

class KotlinCollectionLibrarySupport : LibrarySupportBase() {
    init {
        addOperation(FilterOperation("filter", FilterCallHandler(), true))
        addOperation(FilterOperation("filterNot", FilterCallHandler(), false))
    }

    private fun addOperation(operation: CollectionOperation) {
        addIntermediateOperationsSupport(operation)
        addTerminationOperationsSupport(operation)
    }

    private abstract class CollectionOperation(
        override val name: String,
        handler: BothSemanticsHandler
    ) : IntermediateOperation, TerminalOperation {

        private val wrapper = BothSemanticHandlerWrapper(handler)

        override fun getTraceHandler(callOrder: Int, call: IntermediateStreamCall, dsl: Dsl): IntermediateCallHandler =
            wrapper.createIntermediateHandler(callOrder, call, dsl)

        override fun getTraceHandler(call: TerminatorStreamCall, resultExpression: String, dsl: Dsl): TerminatorCallHandler =
            wrapper.createTerminatorHandler(call, resultExpression, dsl)
    }

    private class FilterOperation(name: String, handler: BothSemanticsHandler, valueToAccept: Boolean) :
        CollectionOperation(name, handler) {
        override val traceInterpreter: CallTraceInterpreter = FilterTraceInterpreter(valueToAccept)
        override val valuesOrderResolver: ValuesOrderResolver = FilterResolver()
    }
}

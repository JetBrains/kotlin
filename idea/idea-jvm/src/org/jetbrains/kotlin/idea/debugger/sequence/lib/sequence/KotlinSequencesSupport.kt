// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.lib.sequence

import com.intellij.debugger.streams.lib.IntermediateOperation
import com.intellij.debugger.streams.lib.impl.*
import com.intellij.debugger.streams.resolve.AppendResolver
import com.intellij.debugger.streams.resolve.PairMapResolver
import com.intellij.debugger.streams.trace.impl.handler.unified.DistinctTraceHandler
import com.intellij.debugger.streams.trace.impl.interpret.SimplePeekCallTraceInterpreter
import org.jetbrains.kotlin.idea.debugger.sequence.resolve.ChunkedResolver
import org.jetbrains.kotlin.idea.debugger.sequence.resolve.FilteredMapResolver
import org.jetbrains.kotlin.idea.debugger.sequence.resolve.WindowedResolver
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.sequence.FilterIsInstanceHandler
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.sequence.KotlinDistinctByHandler

class KotlinSequencesSupport : LibrarySupportBase() {
    init {
        addIntermediateOperationsSupport(
            *filterOperations(
                "filter", "filterNot", "filterIndexed",
                "drop", "dropWhile", "minus", "minusElement", "take", "takeWhile", "onEach", "asSequence"
            )
        )

        addIntermediateOperationsSupport(FilterIsInstanceOperationHandler())

        addIntermediateOperationsSupport(
            *mapOperations(
                "map", "mapIndexed", "requireNoNulls", "withIndex",
                "zip", "constrainOnce"
            )
        )

        addIntermediateOperationsSupport(*flatMapOperations("flatMap", "flatten"))

        addIntermediateOperationsSupport(*sortedOperations("sorted", "sortedBy", "sortedDescending", "sortedWith"))

        addIntermediateOperationsSupport(DistinctOperation("distinct", ::DistinctTraceHandler))
        addIntermediateOperationsSupport(DistinctOperation("distinctBy", ::KotlinDistinctByHandler))

        addIntermediateOperationsSupport(ConcatOperation("plus", AppendResolver()))
        addIntermediateOperationsSupport(ConcatOperation("plusElement", AppendResolver()))

        addIntermediateOperationsSupport(OrderBasedOperation("zipWithNext", PairMapResolver()))

        addIntermediateOperationsSupport(OrderBasedOperation("mapNotNull", FilteredMapResolver()))
        addIntermediateOperationsSupport(OrderBasedOperation("chunked", ChunkedResolver()))
        addIntermediateOperationsSupport(OrderBasedOperation("windowed", WindowedResolver()))
    }

    private fun filterOperations(vararg names: String): Array<IntermediateOperation> =
        names.map { FilterOperation(it) }.toTypedArray()

    private fun mapOperations(vararg names: String): Array<IntermediateOperation> =
        names.map { MappingOperation(it) }.toTypedArray()

    private fun flatMapOperations(vararg names: String): Array<IntermediateOperation> =
        names.map { FlatMappingOperation(it) }.toTypedArray()

    private fun sortedOperations(vararg names: String): Array<IntermediateOperation> =
        names.map { SortedOperation(it) }.toTypedArray()

    private class FilterIsInstanceOperationHandler : IntermediateOperationBase(
        "filterIsInstance", ::FilterIsInstanceHandler,
        SimplePeekCallTraceInterpreter(), FilteredMapResolver()
    )
}
// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.sequence

import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.trace.impl.handler.unified.HandlerBase
import com.intellij.debugger.streams.trace.impl.handler.unified.PeekTraceHandler
import com.intellij.debugger.streams.wrapper.CallArgument
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.impl.CallArgumentImpl
import com.intellij.debugger.streams.wrapper.impl.IntermediateStreamCallImpl
import com.intellij.openapi.util.TextRange.EMPTY_RANGE
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes

class FilterIsInstanceHandler(num: Int, call: IntermediateStreamCall, dsl: Dsl) : HandlerBase.Intermediate(dsl) {
    private companion object {
        fun createHandler(num: Int, call: IntermediateStreamCall, dsl: Dsl): HandlerBase.Intermediate =
            if (call.arguments.isEmpty()) MyWithGenericsHandler(num, call, dsl)
            else PeekTraceHandler(num, call.name, call.typeBefore, call.typeAfter, dsl)
    }

    private val filterHandler = createHandler(num, call, dsl)

    // use explicit delegation to avoid issues with navigation

    override fun additionalCallsAfter(): MutableList<IntermediateStreamCall> = filterHandler.additionalCallsAfter()
    override fun additionalCallsBefore(): List<IntermediateStreamCall> = filterHandler.additionalCallsBefore()
    override fun additionalVariablesDeclaration(): List<VariableDeclaration> = filterHandler.additionalVariablesDeclaration()
    override fun getResultExpression(): Expression = filterHandler.resultExpression
    override fun prepareResult(): CodeBlock = filterHandler.prepareResult()
    override fun transformCall(call: IntermediateStreamCall): IntermediateStreamCall = filterHandler.transformCall(call)

    /*
       * Transforms filterIsInstance<ClassName> -> filter { it is ClassName }.map { it as ClassName }
       */
    private class MyWithGenericsHandler(num: Int, private val call: IntermediateStreamCall, dsl: Dsl) : HandlerBase.Intermediate(dsl) {
        private val peekHandler = PeekTraceHandler(num, "filterIsInstance", call.typeBefore, call.typeAfter, dsl)
        override fun additionalCallsAfter(): List<IntermediateStreamCall> {
            val mapperType = functionalType(call.typeBefore.genericTypeName, call.typeAfter.genericTypeName)
            val mapper = CallArgumentImpl(mapperType, "{ x -> x as ${call.typeAfter.genericTypeName} }")
            val result: MutableList<IntermediateStreamCall> = mutableListOf(syntheticMapCall(mapper))
            result.addAll(peekHandler.additionalCallsAfter())
            return result
        }

        override fun additionalCallsBefore(): List<IntermediateStreamCall> = peekHandler.additionalCallsBefore()

        override fun additionalVariablesDeclaration(): List<VariableDeclaration> =
            peekHandler.additionalVariablesDeclaration()

        override fun getResultExpression(): Expression = peekHandler.resultExpression

        override fun prepareResult(): CodeBlock = peekHandler.prepareResult()

        override fun transformCall(call: IntermediateStreamCall): IntermediateStreamCall {
            val typeAfter = call.typeAfter.genericTypeName
            val predicateType = functionalType(typeAfter, KotlinSequenceTypes.BOOLEAN.genericTypeName)
            val predicate = CallArgumentImpl(predicateType, " { x -> x is $typeAfter} ")
            return syntheticFilterCall(predicate)
        }

        private fun syntheticMapCall(mapper: CallArgument): IntermediateStreamCall =
            IntermediateStreamCallImpl("map", listOf(mapper), call.typeBefore, call.typeAfter, EMPTY_RANGE)

        private fun syntheticFilterCall(predicate: CallArgument): IntermediateStreamCall =
            IntermediateStreamCallImpl("filter", listOf(predicate), call.typeBefore, call.typeBefore, EMPTY_RANGE)

        private fun functionalType(argType: String, resultType: String): String {
            return "($argType) -> $resultType"
        }
    }
}
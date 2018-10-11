// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.dsl.*
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.impl.handler.type.ClassTypeImpl
import com.intellij.debugger.streams.wrapper.CallArgument
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.StreamCall
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall
import com.intellij.debugger.streams.wrapper.impl.CallArgumentImpl
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.typeBefore
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.withArgs

class FilterCallHandler : BothSemanticsHandler {
    private companion object {
        const val VALUES_ARRAY_NAME = "objectsInPredicate"
        const val PREDICATE_RESULT_ARRAY_NAME = "filteringResults"

        fun oldPredicateVariableName(order: Int): String = "filterPredicate$order"
    }

    override fun variablesDeclaration(call: StreamCall, order: Int, dsl: Dsl): List<VariableDeclaration> {
        val types = dsl.types

        val timeToObjectMap = dsl.linkedMap(types.INT, call.typeBefore(), call.name + "Values" + order)
        val predicateResultMap = dsl.linkedMap(types.INT, types.BOOLEAN, call.name + "PredicateValue" + order)
        val predicate = call.arguments[0]
        // TODO: use generic types in CallArgument
        val oldFilterPredicate = dsl.variable(ClassTypeImpl(predicate.type), oldPredicateVariableName(order))

        return listOf(
            timeToObjectMap.defaultDeclaration(), predicateResultMap.defaultDeclaration(),
            dsl.declaration(oldFilterPredicate, TextExpression(predicate.text), false)
        )
    }

    override fun prepareResult(dsl: Dsl, variables: List<Variable>): CodeBlock {
        val values = variables[0] as MapVariable
        val filterResult = variables[1] as MapVariable
        return dsl.block {
            add(values.convertToArray(dsl, VALUES_ARRAY_NAME))
            add(filterResult.convertToArray(dsl, PREDICATE_RESULT_ARRAY_NAME))
        }
    }

    override fun additionalCallsBefore(call: StreamCall, dsl: Dsl): List<IntermediateStreamCall> = emptyList()

    override fun additionalCallsAfter(call: StreamCall, dsl: Dsl): List<IntermediateStreamCall> = emptyList()

    override fun getResultExpression(call: StreamCall, dsl: Dsl, variables: List<Variable>): Expression {
        return dsl.newArray(dsl.types.ANY, TextExpression(VALUES_ARRAY_NAME), TextExpression(PREDICATE_RESULT_ARRAY_NAME))
    }

    override fun transformAsIntermediateCall(call: IntermediateStreamCall, variables: List<Variable>, dsl: Dsl): IntermediateStreamCall {
        return call.withArgs(listOf(createNewPredicate(variables, dsl)))
    }

    override fun transformAsTerminalCall(call: TerminatorStreamCall, variables: List<Variable>, dsl: Dsl): TerminatorStreamCall {
        return call.withArgs(listOf(createNewPredicate(variables, dsl)))
    }

    private fun createNewPredicate(variables: List<Variable>, dsl: Dsl): CallArgument {
        val valuesMap = variables[0] as MapVariable
        val filteringMap = variables[1] as MapVariable
        val oldPredicate = variables[2]
        val newPredicate = dsl.lambda("value") {
            +dsl.updateTime()
            +valuesMap.set(dsl.currentTime(), lambdaArg)
            val filterResult = dsl.variable(dsl.types.BOOLEAN, "result")
            declare(filterResult, oldPredicate.call("invoke", lambdaArg), false)
            +filteringMap.set(dsl.currentTime(), filterResult)
            +dsl.updateTime() // reserve unique time for mapped value
            doReturn(filterResult)
        }.toCode()

        return CallArgumentImpl(oldPredicate.type.genericTypeName, newPredicate)
    }
}
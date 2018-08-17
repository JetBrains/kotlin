// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.sequence

import com.intellij.debugger.streams.trace.dsl.*
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.impl.handler.type.ClassTypeImpl
import com.intellij.debugger.streams.trace.impl.handler.unified.HandlerBase
import com.intellij.debugger.streams.trace.impl.handler.unified.PeekTraceHandler
import com.intellij.debugger.streams.wrapper.CallArgument
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.impl.CallArgumentImpl
import com.intellij.debugger.streams.wrapper.impl.IntermediateStreamCallImpl
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes

/**
 * Based on com.intellij.debugger.streams.trace.impl.handler.unified.DistinctByKeyHandler
 */
class KotlinDistinctByHandler(callNumber: Int, private val call: IntermediateStreamCall, dsl: Dsl) : HandlerBase.Intermediate(dsl) {
    private companion object {
        const val KEY_EXTRACTOR_VARIABLE_PREFIX = "keyExtractor"
        const val TRANSITIONS_ARRAY_NAME = "transitionsArray"
    }

    private val peekHandler = PeekTraceHandler(callNumber, "distinctBy", call.typeBefore, call.typeAfter, dsl)
    private val keyExtractor: CallArgument
    private val extractorVariable: Variable
    private val beforeTimes = dsl.list(dsl.types.INT, call.name + callNumber + "BeforeTimes")
    private val beforeValues = dsl.list(call.typeBefore, call.name + callNumber + "BeforeValues")
    private val keys = dsl.list(KotlinSequenceTypes.NULLABLE_ANY, call.name + callNumber + "Keys")
    private val time2ValueAfter = dsl.linkedMap(dsl.types.INT, call.typeAfter, call.name + callNumber + "after")

    init {
        val arguments = call.arguments
        assert(arguments.isNotEmpty(), { "Key extractor is not specified" })
        keyExtractor = arguments.first()
        extractorVariable = dsl.variable(ClassTypeImpl(keyExtractor.type), KEY_EXTRACTOR_VARIABLE_PREFIX + callNumber)
    }

    override fun additionalVariablesDeclaration(): List<VariableDeclaration> {
        val extractor = dsl.declaration(extractorVariable, TextExpression(keyExtractor.text), false)
        val variables =
            mutableListOf(
                extractor, beforeTimes.defaultDeclaration(), beforeValues.defaultDeclaration(),
                time2ValueAfter.defaultDeclaration(), keys.defaultDeclaration()
            )
        variables.addAll(peekHandler.additionalVariablesDeclaration())

        return variables
    }

    override fun transformCall(call: IntermediateStreamCall): IntermediateStreamCall {
        val newKeyExtractor = dsl.lambda("x") {
            val key = dsl.variable(KotlinSequenceTypes.NULLABLE_ANY, "key")
            declare(key, extractorVariable.call("invoke", lambdaArg), false)
            statement { beforeTimes.add(dsl.currentTime()) }
            statement { beforeValues.add(lambdaArg) }
            statement { keys.add(key) }
            doReturn(key)
        }.toCode()
        return call.updateArguments(listOf(CallArgumentImpl(keyExtractor.type, newKeyExtractor)))
    }

    override fun prepareResult(): CodeBlock {
        val keys2TimesBefore = dsl.map(KotlinSequenceTypes.NULLABLE_ANY, dsl.types.list(dsl.types.INT), "keys2Times")
        val transitions = dsl.map(dsl.types.INT, dsl.types.INT, "transitionsMap")
        return dsl.block {
            add(peekHandler.prepareResult())
            declare(keys2TimesBefore.defaultDeclaration())
            declare(transitions.defaultDeclaration())

            integerIteration(keys.size(), block@ this) {
                val key = declare(variable(KotlinSequenceTypes.NULLABLE_ANY, "key"), keys.get(loopVariable), false)
                val lst = list(dsl.types.INT, "lst")
                declare(lst, keys2TimesBefore.computeIfAbsent(key, lambda("k") {
                    doReturn(newList(types.INT))
                }), false)
                statement { lst.add(beforeTimes.get(loopVariable)) }
            }

            forEachLoop(variable(types.INT, "afterTime"), time2ValueAfter.keys()) {
                val afterTime = loopVariable
                val valueAfter = declare(variable(call.typeAfter, "valueAfter"), time2ValueAfter.get(loopVariable), false)
                val key = declare(variable(KotlinSequenceTypes.NULLABLE_ANY, "key"), nullExpression, true)
                integerIteration(beforeTimes.size(), forEachLoop@ this) {
                    ifBranch((valueAfter same beforeValues.get(loopVariable)) and !transitions.contains(beforeTimes.get(loopVariable))) {
                        key assign keys.get(loopVariable)
                        statement { breakIteration() }
                    }
                }

                forEachLoop(variable(types.INT, "beforeTime"), keys2TimesBefore.get(key)) {
                    statement { transitions.set(loopVariable, afterTime) }
                }
            }

            add(transitions.convertToArray(this, "transitionsArray"))
        }
    }

    override fun getResultExpression(): Expression =
        dsl.newArray(dsl.types.ANY, peekHandler.resultExpression, TextExpression(TRANSITIONS_ARRAY_NAME))

    override fun additionalCallsBefore(): List<IntermediateStreamCall> = peekHandler.additionalCallsBefore()

    override fun additionalCallsAfter(): List<IntermediateStreamCall> {
        val callsAfter = ArrayList(peekHandler.additionalCallsAfter())
        val lambda = dsl.lambda("x") {
            doReturn(time2ValueAfter.set(dsl.currentTime(), lambdaArg))
        }

        callsAfter.add(dsl.createPeekCall(call.typeAfter, lambda.toCode()))
        return callsAfter
    }

    private fun CodeContext.integerIteration(border: Expression, block: CodeBlock, init: ForLoopBody.() -> Unit) {
        block.forLoop(
            declaration(variable(types.INT, "i"), TextExpression("0"), true),
            TextExpression("i < ${border.toCode()}"),
            TextExpression("i = i + 1"), init
        )
    }

    private fun IntermediateStreamCall.updateArguments(args: List<CallArgument>): IntermediateStreamCall =
        IntermediateStreamCallImpl("distinctBy", args, typeBefore, typeAfter, textRange)
}
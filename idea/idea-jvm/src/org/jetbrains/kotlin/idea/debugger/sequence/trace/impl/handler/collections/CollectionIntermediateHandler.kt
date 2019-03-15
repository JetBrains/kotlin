// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.IntermediateCallHandler
import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall

class CollectionIntermediateHandler(
    order: Int,
    private val call: IntermediateStreamCall,
    private val dsl: Dsl,
    private val internalHandler: BothSemanticsHandler
) : IntermediateCallHandler, CollectionHandlerBase(order, dsl, call, internalHandler) {

    override fun prepareResult(): CodeBlock {
        return internalHandler.prepareResult(dsl, variables)
    }

    override fun additionalCallsBefore(): List<IntermediateStreamCall> {
        return internalHandler.additionalCallsBefore(call, dsl)
    }

    override fun additionalCallsAfter(): List<IntermediateStreamCall> {
        return internalHandler.additionalCallsAfter(call, dsl)
    }

    override fun transformCall(call: IntermediateStreamCall): IntermediateStreamCall {
        return internalHandler.transformAsIntermediateCall(call, variables, dsl)
    }

    override fun getResultExpression(): Expression {
        return internalHandler.getResultExpression(call, dsl, variables)
    }
}
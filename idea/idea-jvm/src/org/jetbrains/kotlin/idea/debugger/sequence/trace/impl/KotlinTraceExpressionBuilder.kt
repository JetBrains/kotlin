// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl

import com.intellij.debugger.streams.lib.HandlerFactory
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.impl.TraceExpressionBuilderBase
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.openapi.diagnostic.Logger

class KotlinTraceExpressionBuilder(dsl: Dsl, handlerFactory: HandlerFactory) : TraceExpressionBuilderBase(dsl, handlerFactory) {
    private companion object {
        private val LOG = Logger.getInstance(KotlinTraceExpressionBuilder::class.java)
    }

    override fun createTraceExpression(chain: StreamChain): String {
        val expression = super.createTraceExpression(chain)
        val resultDeclaration = dsl.declaration(dsl.variable(dsl.types.nullable { ANY }, resultVariableName), dsl.nullExpression, true)
        val result = "${resultDeclaration.toCode()}\n " +
                "$expression\n" +
                resultVariableName

        LOG.info("trace expression: \n$result")

        return result
    }
}
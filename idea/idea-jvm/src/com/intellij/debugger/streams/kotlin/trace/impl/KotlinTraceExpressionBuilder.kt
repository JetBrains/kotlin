/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.debugger.streams.kotlin.trace.impl

import com.intellij.debugger.streams.lib.HandlerFactory
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.impl.TraceExpressionBuilderBase
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.openapi.diagnostic.Logger

/**
 * @author Vitaliy.Bibaev
 */
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
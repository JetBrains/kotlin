// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.dsl.*
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.StreamCall
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall

interface BothSemanticsHandler {
    fun variablesDeclaration(call: StreamCall, order: Int, dsl: Dsl): List<VariableDeclaration>

    fun prepareResult(dsl: Dsl, variables: List<Variable>): CodeBlock

    fun additionalCallsBefore(call: StreamCall, dsl: Dsl): List<IntermediateStreamCall>

    fun additionalCallsAfter(call: StreamCall, dsl: Dsl): List<IntermediateStreamCall>

    fun transformAsIntermediateCall(call: IntermediateStreamCall, variables: List<Variable>, dsl: Dsl): IntermediateStreamCall

    fun transformAsTerminalCall(call: TerminatorStreamCall, variables: List<Variable>, dsl: Dsl): TerminatorStreamCall

    fun getResultExpression(call: StreamCall, dsl: Dsl, variables: List<Variable>): Expression
}
// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.TraceHandler
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.Variable
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.wrapper.StreamCall

abstract class CollectionHandlerBase(
    order: Int, private val dsl: Dsl,
    private val call: StreamCall, private val internalHandler: BothSemanticsHandler
) : TraceHandler {
    private val declarations: List<VariableDeclaration> = internalHandler.variablesDeclaration(call, order, dsl)
    protected val variables: List<Variable> = declarations.map(VariableDeclaration::variable)

    override fun additionalVariablesDeclaration(): List<VariableDeclaration> = declarations

    override fun getResultExpression(): Expression = internalHandler.getResultExpression(call, dsl, variables)
}
package com.intellij.debugger.streams.kotlin.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.IntermediateCallHandler
import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall

/**
 * @author Vitaliy.Bibaev
 */
class CollectionIntermediateHandler(
    private val order: Int,
    private val call: IntermediateStreamCall,
    private val dsl: Dsl,
    private val internalHandler: BothSemanticsHandler)
  : IntermediateCallHandler {
  override fun additionalVariablesDeclaration(): MutableList<VariableDeclaration> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun prepareResult(): CodeBlock {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun additionalCallsBefore(): MutableList<IntermediateStreamCall> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun additionalCallsAfter(): MutableList<IntermediateStreamCall> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getResultExpression(): Expression {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
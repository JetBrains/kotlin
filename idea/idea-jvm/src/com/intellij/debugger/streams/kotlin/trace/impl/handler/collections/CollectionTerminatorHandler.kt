package com.intellij.debugger.streams.kotlin.trace.impl.handler.collections

import com.intellij.debugger.streams.trace.TerminatorCallHandler
import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall

/**
 * @author Vitaliy.Bibaev
 */
class CollectionTerminatorHandler(private val internalHandler: TerminatorStreamCall,
                                  private val resultExpression: String,
                                  private val dsl: Dsl,
                                  private val handler: BothSemanticsHandler)
  : TerminatorCallHandler {
  override fun additionalVariablesDeclaration(): MutableList<VariableDeclaration> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun prepareResult(): CodeBlock {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun additionalCallsBefore(): MutableList<IntermediateStreamCall> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getResultExpression(): Expression {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
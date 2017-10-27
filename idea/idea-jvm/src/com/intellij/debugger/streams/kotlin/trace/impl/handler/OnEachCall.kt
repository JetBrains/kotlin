package com.intellij.debugger.streams.kotlin.trace.impl.handler

import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.debugger.streams.wrapper.CallArgument
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.StreamCallType
import com.intellij.debugger.streams.wrapper.impl.CallArgumentImpl
import com.intellij.openapi.util.TextRange

/**
 * @author Vitaliy.Bibaev
 */
class OnEachCall(private val elementsType: GenericType, lambda: String) : IntermediateStreamCall {
  private val args: List<CallArgument>

  init {
    args = listOf(CallArgumentImpl(KotlinTypes.ANY.genericTypeName, lambda))
  }

  override fun getArguments(): List<CallArgument> = args

  override fun getName(): String = "onEach"

  override fun getType(): StreamCallType = StreamCallType.INTERMEDIATE

  override fun getTextRange(): TextRange = TextRange.EMPTY_RANGE

  override fun getTypeBefore(): GenericType = elementsType

  override fun getTypeAfter(): GenericType = elementsType
}
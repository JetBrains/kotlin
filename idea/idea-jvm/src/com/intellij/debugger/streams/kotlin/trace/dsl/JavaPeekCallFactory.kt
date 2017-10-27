package com.intellij.debugger.streams.kotlin.trace.dsl

import com.intellij.debugger.streams.trace.impl.handler.PeekCall
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall

/**
 * @author Vitaliy.Bibaev
 */
class JavaPeekCallFactory : PeekCallFactory {
  override fun createPeekCall(elementsType: GenericType, lambda: String): IntermediateStreamCall =
      PeekCall(lambda, elementsType)
}
package com.intellij.debugger.streams.kotlin.trace.dsl

import com.intellij.debugger.streams.kotlin.trace.impl.handler.OnEachCall
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall

/**
 * @author Vitaliy.Bibaev
 */
class KotlinCollectionsPeekCallFactory : PeekCallFactory {
  override fun createPeekCall(elementsType: GenericType, lambda: String): IntermediateStreamCall =
      OnEachCall(elementsType, lambda)
}
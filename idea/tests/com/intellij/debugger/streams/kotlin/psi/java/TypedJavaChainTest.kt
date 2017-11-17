package com.intellij.debugger.streams.kotlin.psi.java

import com.intellij.debugger.streams.kotlin.psi.TypedChainTestCase
import com.intellij.debugger.streams.lib.impl.StandardLibrarySupportProvider
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
class TypedJavaChainTest : TypedChainTestCase("streams/positive/types") {
  override val kotlinChainBuilder: StreamChainBuilder = StandardLibrarySupportProvider().chainBuilder
}
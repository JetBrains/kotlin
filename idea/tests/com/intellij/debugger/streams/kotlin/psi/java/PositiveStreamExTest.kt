package com.intellij.debugger.streams.kotlin.psi.java

import com.intellij.debugger.streams.kotlin.KotlinPsiChainBuilderTestCase
import com.intellij.debugger.streams.kotlin.lib.StreamExLibrarySupportProvider
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
class PositiveStreamExTest : KotlinPsiChainBuilderTestCase.Positive("streams/positive/streamex") {
  override val kotlinChainBuilder: StreamChainBuilder = StreamExLibrarySupportProvider().chainBuilder

  fun testSimple() {
    doTest()
  }
}

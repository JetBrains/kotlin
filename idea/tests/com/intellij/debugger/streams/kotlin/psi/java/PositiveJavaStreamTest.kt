package com.intellij.debugger.streams.kotlin.psi.java

import com.intellij.debugger.streams.kotlin.KotlinPsiChainBuilderTestCase
import com.intellij.debugger.streams.kotlin.lib.JavaStandardLibrarySupportProvider
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
class PositiveJavaStreamTest : KotlinPsiChainBuilderTestCase.Positive("streams/positive") {
  override val kotlinChainBuilder: StreamChainBuilder = JavaStandardLibrarySupportProvider().chainBuilder

  fun testSimple() {
    doTest()
  }
}
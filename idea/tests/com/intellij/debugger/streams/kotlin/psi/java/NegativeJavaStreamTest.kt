// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.java

import com.intellij.debugger.streams.kotlin.KotlinPsiChainBuilderTestCase
import com.intellij.debugger.streams.kotlin.lib.JavaStandardLibrarySupportProvider
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
class NegativeJavaStreamTest : KotlinPsiChainBuilderTestCase.Negative("streams/negative") {
  override val kotlinChainBuilder: StreamChainBuilder = JavaStandardLibrarySupportProvider().chainBuilder

  fun testFakeStream() = doTest()

  fun testWithoutTerminalOperation() = doTest()

  fun testNoBreakpoint() = doTest()

  fun testBreakpointOnMethod() = doTest()
  fun testBreakpointOnIfCondition() = doTest()
  fun testBreakpointOnNewScope() = doTest()
  fun testBreakpointOnElseBranch() = doTest()

  fun testInLambda() = doTest()
  fun testInLambdaWithBody() = doTest()
  fun testInAnonymous() = doTest()

  fun testAfterStatement() = doTest()

  fun testInPreviousStatement() = doTest()
  fun testInNextStatement() = doTest()

  fun testIdea173415() = doTest()
}
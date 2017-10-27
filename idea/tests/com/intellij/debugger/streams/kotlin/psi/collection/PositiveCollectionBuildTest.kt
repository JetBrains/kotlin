package com.intellij.debugger.streams.kotlin.psi.collection

import com.intellij.debugger.streams.kotlin.KotlinPsiChainBuilderTestCase
import com.intellij.debugger.streams.kotlin.lib.KotlinCollectionSupportProvider
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
class PositiveCollectionBuildTest : KotlinPsiChainBuilderTestCase.Positive("collection/positive") {
  override val kotlinChainBuilder: StreamChainBuilder = KotlinCollectionSupportProvider().chainBuilder

  fun testIntermediateIsLastCall() = doTest()
  fun testOnlyFilterCall() = doTest()
  fun testTerminationCallUsed() = doTest()
  fun testOnlyTerminationCallUsed() = doTest()
}
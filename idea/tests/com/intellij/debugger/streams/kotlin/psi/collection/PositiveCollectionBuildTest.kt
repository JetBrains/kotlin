// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.collection

import com.intellij.debugger.streams.kotlin.KotlinPsiChainBuilderTestCase
import com.intellij.debugger.streams.kotlin.lib.collections.KotlinCollectionSupportProvider
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
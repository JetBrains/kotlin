// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.java

import com.intellij.debugger.streams.kotlin.KotlinPsiChainBuilderTestCase
import com.intellij.debugger.streams.kotlin.lib.JavaStandardLibrarySupportProvider
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
abstract class PositiveJavaStreamTest(subDirectory: String) : KotlinPsiChainBuilderTestCase.Positive("streams/positive/$subDirectory") {
  override val kotlinChainBuilder: StreamChainBuilder = JavaStandardLibrarySupportProvider().chainBuilder
}
// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.debugger.streams.kotlin.exec.java

import com.intellij.debugger.streams.kotlin.exec.KotlinEvaluationTestCase
import com.intellij.debugger.streams.kotlin.lib.JavaStandardLibrarySupportProvider
import com.intellij.debugger.streams.lib.LibrarySupportProvider


/**
 * @author Vitaliy.Bibaev
 */
class JavaStreamTest : KotlinEvaluationTestCase() {
  override val appName: String = "java"
  override val librarySupport: LibrarySupportProvider = JavaStandardLibrarySupportProvider()

  fun testSimple() {
    doTest(false)
  }
}
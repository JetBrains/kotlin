// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.exec.collection

import com.intellij.debugger.streams.kotlin.exec.KotlinTraceEvaluationTestCase
import com.intellij.debugger.streams.kotlin.lib.KotlinCollectionSupportProvider
import com.intellij.debugger.streams.lib.LibrarySupportProvider

/**
 * @author Vitaliy.Bibaev
 */
abstract class CollectionTestCase(private val packageName: String) : KotlinTraceEvaluationTestCase() {
  override val appName: String = "collection"
  override val librarySupport: LibrarySupportProvider = KotlinCollectionSupportProvider()

  override fun createLocalProcess(className: String) = super.createLocalProcess("$packageName.$className")

  protected fun doTestWithResult() = doTest(false)
}

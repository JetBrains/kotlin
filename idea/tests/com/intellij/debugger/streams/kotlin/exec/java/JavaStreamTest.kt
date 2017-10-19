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
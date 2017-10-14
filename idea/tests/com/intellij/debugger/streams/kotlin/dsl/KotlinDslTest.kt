package com.intellij.debugger.streams.kotlin.dsl

import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinStatementFactory
import com.intellij.debugger.streams.test.DslTestCase
import com.intellij.debugger.streams.trace.dsl.impl.DslImpl

/**
 * @author Vitaliy.Bibaev
 */
class KotlinDslTest : DslTestCase(DslImpl(KotlinStatementFactory())) {
  override fun getTestDataPath(): String {
    return "testData/dsl"
  }
}
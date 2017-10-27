// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.dsl

import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinCollectionsPeekCallFactory
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinStatementFactory
import com.intellij.debugger.streams.test.DslTestCase
import com.intellij.debugger.streams.trace.dsl.impl.DslImpl

/**
 * @author Vitaliy.Bibaev
 */
class KotlinDslTest : DslTestCase(DslImpl(KotlinStatementFactory(KotlinCollectionsPeekCallFactory()))) {
  override fun getTestDataPath(): String {
    return "testData/dsl"
  }
}
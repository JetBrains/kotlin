// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.java

import com.intellij.debugger.streams.kotlin.lib.JavaStandardLibrarySupportProvider
import com.intellij.debugger.streams.kotlin.psi.TypedChainTestCase
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes.DOUBLE
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes.INT
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes.LONG
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes.NULLABLE_ANY
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
class TypedJavaChainTest : TypedChainTestCase("streams/positive/types") {
  override val kotlinChainBuilder: StreamChainBuilder = JavaStandardLibrarySupportProvider().chainBuilder

  fun testOneCall() = doTest(NULLABLE_ANY)
  fun testMapToSame() = doTest(NULLABLE_ANY, NULLABLE_ANY)
  fun testPrimitiveOneCall() = doTest(INT)
  fun testPrimitiveMapToSame() = doTest(LONG, LONG)

  fun testMapToPrimitive() = doTest(NULLABLE_ANY, DOUBLE)
  fun testMapToObj() = doTest(DOUBLE, NULLABLE_ANY)
  fun testMapPrimitiveToPrimitive() = doTest(LONG, INT)

  fun testFewTransitions() = doTest(NULLABLE_ANY, INT, NULLABLE_ANY, LONG, DOUBLE)
}

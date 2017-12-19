// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.collection

import com.intellij.debugger.streams.kotlin.lib.collections.KotlinCollectionSupportProvider
import com.intellij.debugger.streams.kotlin.psi.TypedChainTestCase
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
class TypedCollectionChainTest : TypedChainTestCase("collection/positive/types") {
  override val kotlinChainBuilder: StreamChainBuilder = KotlinCollectionSupportProvider().chainBuilder

  fun testAny() = doTest(KotlinTypes.ANY)
  fun testNullableAny() = doTest(KotlinTypes.NULLABLE_ANY)

  fun testBoolean() = doTest(KotlinTypes.BOOLEAN)
  fun testNullableBoolean() = doTest(KotlinTypes.NULLABLE_ANY)

  fun testByte() = doTest(KotlinTypes.BYTE)
  fun testNullableByte() = doTest(KotlinTypes.NULLABLE_ANY)

  fun testShort() = doTest(KotlinTypes.SHORT)
  fun testNullableShort() = doTest(KotlinTypes.NULLABLE_ANY)

  fun testInt() = doTest(KotlinTypes.INT)
  fun testNullableInt() = doTest(KotlinTypes.NULLABLE_ANY)

  fun testLong() = doTest(KotlinTypes.LONG)
  fun testNullableLong() = doTest(KotlinTypes.NULLABLE_ANY)

  fun testFloat() = doTest(KotlinTypes.FLOAT)
  fun testNullableFloat() = doTest(KotlinTypes.NULLABLE_ANY)

  fun testDouble() = doTest(KotlinTypes.DOUBLE)
  fun testNullableDouble() = doTest(KotlinTypes.NULLABLE_ANY)

  fun testChar() = doTest(KotlinTypes.CHAR)
  fun testNullableChar() = doTest(KotlinTypes.NULLABLE_ANY)

  fun testNullableAnyToPrimitive() = doTest(KotlinTypes.NULLABLE_ANY, KotlinTypes.BOOLEAN)
  fun testPrimitiveToNullableAny() = doTest(KotlinTypes.INT, KotlinTypes.NULLABLE_ANY)

  fun testAnyToPrimitive() = doTest(KotlinTypes.ANY, KotlinTypes.INT)
  fun testPrimitiveToAny() = doTest(KotlinTypes.INT, KotlinTypes.ANY)

  fun testNullableToNotNull() = doTest(KotlinTypes.NULLABLE_ANY, KotlinTypes.INT)
  fun testNotNullToNullable() = doTest(KotlinTypes.DOUBLE, KotlinTypes.NULLABLE_ANY)

  fun testFewTransitions1() = doTest(KotlinTypes.BYTE, KotlinTypes.ANY, KotlinTypes.NULLABLE_ANY, KotlinTypes.INT)
  fun testFewTransitions2() = doTest(KotlinTypes.CHAR, KotlinTypes.BOOLEAN, KotlinTypes.DOUBLE, KotlinTypes.ANY)
}
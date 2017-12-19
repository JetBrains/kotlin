// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.exec.collection

/**
 * @author Vitaliy.Bibaev
 */
class FilterOperationsTest : CollectionTestCase("filter") {
  fun testFilterAsIntermediate() = doTestWithResult()
  fun testFilterAsTerminal() = doTestWithResult()

  fun testFilterPrimitive() = doTestWithResult()
  fun testFilterPrimitiveAsTermination() = doTestWithResult()

  fun testFilterPassNothing() = doTestWithResult()
}
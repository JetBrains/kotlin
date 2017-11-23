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
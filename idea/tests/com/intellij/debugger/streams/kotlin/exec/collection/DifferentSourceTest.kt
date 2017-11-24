package com.intellij.debugger.streams.kotlin.exec.collection

/**
 * @author Vitaliy.Bibaev
 */
class DifferentSourceTest : CollectionTestCase("source") {
  fun testCollectionAsSource() = doTestWithResult()
  fun testStringAsSource() = doTestWithResult()
  fun testArrayAsSource() = doTestWithResult()
  fun testPrimitiveArrayAsSource() = doTestWithResult()
}
package com.intellij.debugger.streams.kotlin.exec.collection

/**
 * @author Vitaliy.Bibaev
 */
class DistinctOperationsTest : CollectionTestCase("distinct") {
  fun testDistinct() {
    doTestWithResult()
  }

  fun testDistinctBy() {
    doTestWithResult()
  }
}
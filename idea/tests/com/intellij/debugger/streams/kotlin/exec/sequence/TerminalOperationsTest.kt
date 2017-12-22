// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.exec.sequence

/**
 * @author Vitaliy.Bibaev
 */
class TerminalOperationsTest : OperationsTestCase("terminal") {
  fun testAllTrue() = doTestWithResult()
  fun testAllFalse() = doTestWithResult()

  fun testAnyTrue() = doTestWithResult()
  fun testAnyFalse() = doTestWithResult()

  fun testAsIterable() = doTestWithResult()

  fun testAssociate() = doTestWithResult()
  fun testAssociateBy() = doTestWithResult()

  fun testAverage() = doTestWithResult()

  fun testCount() = doTestWithResult()

  fun testElementAt() = doTestWithResult()
  fun testElementAtOrElsePresent() = doTestWithResult()
  fun testElementAtOrElseAbsent() = doTestWithResult()
  fun testElementAtOrNull() = doTestWithResult()

  fun testFindPresent() = doTestWithResult()
  fun testFindAbsent() = doTestWithResult()

  fun testFindLastPresent() = doTestWithResult()
  fun testFindLastAbsent() = doTestWithResult()

  fun testFirst() = doTestWithResult()

  fun testFirstOrNullPresent() = doTestWithResult()
  fun testFirstOrNullAbsent() = doTestWithResult()

  fun testGroupingBy() = doTestWithResult()

  fun testIndexOfPresent() = doTestWithResult()
  fun testIndexOfAbsent() = doTestWithResult()

  fun testIndexOfFirstPresent() = doTestWithResult()
  fun testIndexOfFirstAbsent() = doTestWithResult()

  fun testIndexOfLastPresent() = doTestWithResult()
  fun testIndexOfLastAbsent() = doTestWithResult()

  fun testLast() = doTestWithResult()
  fun testLastIndexOf() = doTestWithResult()
  fun testLastOrNullPresent() = doTestWithResult()
  fun testLastOrNullAbsent() = doTestWithResult()

  fun testMaxPresent() = doTestWithResult()
  fun testMaxAbsent() = doTestWithResult()
  fun testMaxBy() = doTestWithResult()
  fun testMaxWith() = doTestWithResult()

  fun testMinPresent() = doTestWithResult()
  fun testMinAbsent() = doTestWithResult()
  fun testMinBy() = doTestWithResult()
  fun testMinWith() = doTestWithResult()

  fun testNonePresent() = doTestWithResult()
  fun testNoneAbsent() = doTestWithResult()

  fun testPartition() = doTestWithResult()

  fun testSingle() = doTestWithResult()
  fun testSingleOrNullPresent() = doTestWithResult()
  fun testSingleOrNullAbsent() = doTestWithResult()

  fun testToCollection() = doTestWithResult()
  fun testToHashSet() = doTestWithResult()
  fun testToMutableSet() = doTestWithResult()
  fun testToList() = doTestWithResult()
  fun testToMutableList() = doTestWithResult()
  fun testToSet() = doTestWithResult()
  fun testToSortedSet() = doTestWithResult()
}
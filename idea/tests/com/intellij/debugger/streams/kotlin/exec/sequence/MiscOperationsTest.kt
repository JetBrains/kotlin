// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.exec.sequence

/**
 * @author Vitaliy.Bibaev
 */
class MiscOperationsTest : OperationsTestCase("misc") {
  fun testConstrainOnce() = doTestWithResult()

  fun testRequireNoNulls() = doTestWithResult()

  fun testOnEach() = doTestWithResult()

  fun testZipWithSame() = doTestWithResult()
  fun testZipWithGreater() = doTestWithResult()
  fun testZipWithLesser() = doTestWithResult()

  fun testZipWithNextSingle() = doTestWithResult()
  fun testZipWithNextMany() = doTestWithResult()

  fun testChunked() = doTestWithResult()
  fun testChunkedWithTransform() = doTestWithResult()

  fun testWindowed() = doTestWithResult()
  fun testWindowedWithPartial() = doTestWithResult()
  fun testWindowedWithBigStep() = doTestWithResult()
  fun testWindowedWithStep() = doTestWithResult()
}
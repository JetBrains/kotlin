/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.array3

import kotlin.test.*

import kotlin.native.*

@Test fun runTest() {
    val data = immutableBlobOf(0x1, 0x2, 0x3, 0x7, 0x8, 0x9, 0x80, 0xff)
    for (b in data) {
        print("$b ")
    }
    println()

    val dataClone = data.toByteArray()
    dataClone.map { print("$it ") }
    println()
}
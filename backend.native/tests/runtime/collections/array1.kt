/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.array1

import kotlin.test.*

@Test fun runTest() {
    val byteArray = ByteArray(5)
    byteArray[1] = 2
    byteArray[3] = 4
    println(byteArray[3].toString() + " "  + byteArray[1].toString())

    val shortArray = ShortArray(2)
    shortArray[0] = -1
    shortArray[1] = 1
    print(shortArray[1].toString())
    print(shortArray[0].toString())
    println()

    val intArray = IntArray(7)
    intArray[1] = 9
    intArray[3] = 6
    print(intArray[3].toString())
    print(intArray[1].toString())
    println()

    val longArray = LongArray(9)
    longArray[8] = 8
    longArray[3] = 3
    print(longArray[3].toString())
    print(longArray[8].toString())
    println()
}
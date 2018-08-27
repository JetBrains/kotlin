/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.tostring0

import kotlin.test.*

@Test fun runTest() {
    println(127.toByte().toString())
    println(255.toByte().toString())
    println(239.toShort().toString())
    println('A'.toString())
    println('Ё'.toString())
    println('ト'.toString())
    println(1122334455.toString())
    println(112233445566778899.toString())
    // Here we differ from Java, as have no dtoa() yet.
    println(3.14159265358.toString())
    // Here we differ from Java, as have no dtoa() yet.
    println(1e27.toFloat().toString())
    println(1e-300.toDouble().toString())
    println(true.toString())
    println(false.toString())
}
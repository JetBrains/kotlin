/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.boxing.boxing7

import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Any) {
    val argAsInt = try {
        arg as Int
    } catch (e: ClassCastException) {
        0
    }
    printInt(argAsInt)
}

@Test fun runTest() {
    foo(1)
    foo("Hello")
}
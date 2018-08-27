/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.boxing.boxing9

import kotlin.test.*

fun foo(vararg args: Any?) {
    for (arg in args) {
        println(arg.toString())
    }
}

fun bar(vararg args: Any?) {
    foo(1, *args, 2, *args, 3)
}

@Test fun runTest() {
    bar(null, true, "Hello")
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.coercionToUnit

import kotlin.test.*

fun <T> myRun(action: () -> T): T = action()

fun foo(n: Number, b: Boolean) {
    n.let {
        if (b) return@let

        myRun() { 42 }
    }
}

@Test fun runTest() {
    println(foo(42, false))
}

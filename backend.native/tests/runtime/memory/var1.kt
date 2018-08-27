/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.var1

import kotlin.test.*

class Integer(val value: Int) {
    operator fun inc() = Integer(value + 1)
}

fun foo(x: Any, y: Any) {
    x.use()
    y.use()
}

@Test fun runTest() {
    var x = Integer(0)

    for (i in 0..1) {
        val c = Integer(0)
        if (i == 0) x = c
    }

    // x refcount is 1.

    foo(x, ++x)
}

fun Any?.use() {
    var x = this
}
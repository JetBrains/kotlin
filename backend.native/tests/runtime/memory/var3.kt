/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.var3

import kotlin.test.*

@Test fun runTest() {
    foo().use()
}

fun foo(): Any {
    var x = Any()

    for (i in 0..1) {
        val c = Any()
        if (i == 0) x = c
    }

    // x refcount is 1.

    try {
        return x
    } finally {
        x = Any()
    }
}

fun Any?.use() {
    var x = this
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.lazy1

import kotlin.test.*

import kotlin.native.concurrent.*

class Leak {
    val leak by lazy { this }
}

@Test fun runTest() {
    assertFailsWith<InvalidMutabilityException> {
        for (i in 1..100)
            Leak().freeze().leak
    }
    println("OK")
}

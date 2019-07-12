/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.freeze3

import kotlin.test.*

import kotlin.native.concurrent.*

object AnObject {
    var x = 1
}

@ThreadLocal
object Mutable {
    var x = 2
}

@Test fun runTest() {
    assertEquals(1, AnObject.x)
    if (Platform.memoryModel == MemoryModel.STRICT) {
        assertFailsWith<InvalidMutabilityException> {
            AnObject.x++
        }
        assertEquals(1, AnObject.x)
    } else {
        AnObject.x++
        assertEquals(2, AnObject.x)
    }

    Mutable.x++
    assertEquals(3, Mutable.x)
    println("OK")
}

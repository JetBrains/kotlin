/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.initializers7

import kotlin.test.*

import kotlin.random.Random

object A {
    val a1 = Random.nextInt(100)
    val a2 = Random.nextInt(100)
}

object B {
    val b1 = A.a2
    val b2 = C.c1
}

object C {
    val c1 = Random.nextInt(100)
    val c2 = A.a1
    val c3 = B.b1
    val c4 = B.b2
}

@Test fun runTest() {
    assertEquals(A.a1, C.c2)
    assertEquals(A.a2, C.c3)
    assertEquals(C.c1, C.c4)
}

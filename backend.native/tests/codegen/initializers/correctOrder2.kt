/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.initializers.correctOrder2

import kotlin.test.*

class TestClass {
    val x: Int

    val y = 42

    init {
        x = y
    }
}

@Test fun runTest() {
    println(TestClass().x)
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// FILE: 1.kt

package codegen.lambda.lambda14

import kotlin.test.*

@Test fun runTest() {
    assertEquals(foo()(), "foo1")
    assertEquals(foo(0)(), "foo2")
}

fun foo() = { "foo1" }

// FILE: 2.kt

package codegen.lambda.lambda14

fun foo(ignored: Int) = { "foo2" }

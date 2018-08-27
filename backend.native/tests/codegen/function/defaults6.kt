/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaults6

import kotlin.test.*

open class Foo(val x: Int = 42)
class Bar : Foo()

@Test fun runTest() {
    println(Bar().x)
}
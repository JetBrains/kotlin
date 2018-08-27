/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.bridges.test4

import kotlin.test.*

// vtable call + interface call
interface Z {
    fun foo(): Any
}

interface Y {
    fun foo(): Int
}

open class A {
    fun foo(): Int = 42
}

open class B: A(), Z, Y

@Test fun runTest() {
    val z: Z = B()
    val y: Y = z as Y
    println(z.foo().toString())
    println(y.foo().toString())
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.bridges.test12

import kotlin.test.*

abstract class A<in T> {
    abstract fun foo(x: T)
}

class B : A<Int>() {
    override fun foo(x: Int) {
        println("B: $x")
    }
}

class C : A<Any>() {
    override fun foo(x: Any) {
        println("C: $x")
    }
}

fun foo(arg: A<Int>) {
    arg.foo(42)
}

@Test fun runTest() {
    foo(B())
    foo(C())
}
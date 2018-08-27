/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.classDelegation.method

import kotlin.test.*

interface A<T> {
    fun foo(): T
}

class B : A<String> {
    override fun foo() = "OK"
}

class C(a: A<String>) : A<String> by a

fun box(): String {
    val c = C(B())
    val a: A<String> = c
    return c.foo() + a.foo()
}

@Test fun runTest() {
    println(box())
}
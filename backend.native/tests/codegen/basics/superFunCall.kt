/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.superFunCall

import kotlin.test.*

open class C {
    open fun f() = "<fun:C>"
}

class C1: C() {
    override fun f() = super<C>.f() + "<fun:C1>"
}

open class C2: C() {
}

class C3: C2() {
    override fun f() = super<C2>.f() + "<fun:C3>"
}

@Test
fun runTest() {
    println(C1().f())
    println(C3().f())
}
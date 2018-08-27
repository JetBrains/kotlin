/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaults3

import kotlin.test.*

fun foo(a:Int = 2, b:String = "Hello", c:Int = 4):String = "$b-$c$a"
fun foo(a:Int = 3, b:Int = a + 1, c:Int = a + b) = a + b + c

@Test fun runTest() {
    val a = foo(b="Universe")
    if (a != "Universe-42")
        throw Error()

    val b = foo(b = 5)
    if (b != (/* a = */ 3 + /* b = */ 5 + /* c = */ (3 + 5)))
        throw Error()
}
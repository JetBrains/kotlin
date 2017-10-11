package codegen.bridges.linkTest_main

import kotlin.test.*
import codegen.bridges.linkTest.a.*

class B: C()

@Test fun runTest() {
    val b = B()
    println(b.foo())
    val c: C = b
    println(c.foo())
    val a: A<Int> = b
    println(a.foo())
}
package codegen.propertyCallableReference.linkTest_main

import kotlin.test.*

import codegen.propertyCallableReference.linkTest.a.A

@Test fun runTest() {
    val p1 = A::x
    println(p1.get(A(42)))
    val a = A(117)
    val p2 = a::x
    println(p2.get())
}
package codegen.propertyCallableReference.varClass

import kotlin.test.*

class A(var x: Int)

@Test fun runTest() {
    val p1 = A::x
    val a = A(42)
    p1.set(a, 117)
    println(a.x)
    println(p1.get(a))
    val p2 = a::x
    p2.set(42)
    println(a.x)
    println(p2.get())
}
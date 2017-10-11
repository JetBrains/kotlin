package runtime.memory.escape1

import kotlin.test.*

class B(val s: String)

class A {
    val b = B("zzz")
}

fun foo(): B {
    val a = A()
    return a.b
}

@Test fun runTest() {
    println(foo().s)
}
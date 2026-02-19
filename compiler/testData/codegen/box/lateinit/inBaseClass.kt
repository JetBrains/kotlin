// WITH_STDLIB

import kotlin.test.*

class A(val a: Int)

open class B {
    lateinit var a: A
}

class C: B() {
    fun foo() { a = A(42) }
}

fun box(): String {
    val c = C()
    c.foo()
    assertEquals(42, c.a.a)

    return "OK"
}

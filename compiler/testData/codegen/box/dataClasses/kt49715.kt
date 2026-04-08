// CHECK_BYTECODE_LISTING
// WITH_STDLIB

import kotlin.test.*

interface A {
    fun Any.toString(): String = "hello"
}

data class B(val x: Int) : A {
    fun Any.hi() = "hi"
}

fun box(): String {
    val b = B(1)
    assertEquals("B(x=1)", b.toString())
    assertTrue(b == B(1))
    assertTrue(1.hashCode() == b.hashCode())
    return "OK"
}

package codegen.inline.inline24

import kotlin.test.*

fun foo() = println("foo")
fun bar() = println("bar")

inline fun baz(x: Unit = foo(), y: Unit) {}

@Test fun runTest() {
    baz(y = bar())
}

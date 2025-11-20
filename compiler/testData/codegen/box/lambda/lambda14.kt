// WITH_STDLIB

// FILE: 1.kt

import kotlin.test.*

fun box(): String {
    assertEquals(foo()(), "foo1")
    assertEquals(foo(0)(), "foo2")

    return "OK"
}

fun foo() = { "foo1" }

// FILE: 2.kt

fun foo(ignored: Int) = { "foo2" }

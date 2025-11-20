// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val x = foo {
        it + 1
    }
    assertEquals(43, x)
    return "OK"
}

fun foo(f: (Int) -> Int) = f(42)

// WITH_STDLIB

import kotlin.test.*

fun foo(b: Boolean): Int {
    val x: Int
    if (b) {
        x = 1
    } else {
        x = 2
    }

    return x
}

fun box(): String {
    val uninitializedUnused: Int

    assertEquals(1, foo(true))
    assertEquals(2, foo(false))

    return "OK"
}

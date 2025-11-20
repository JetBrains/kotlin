// WITH_STDLIB

import kotlin.test.*

class Z<T>(val x: T)

inline fun<T, R> foo(x: T, f: (T) -> R): R {
    return f(x)
}

fun box(): String {
    val arr = Array(1) { foo(it, ::Z) }
    assertEquals(0, arr[0].x)
    return "OK"
}

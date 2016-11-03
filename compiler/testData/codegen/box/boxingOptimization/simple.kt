// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

inline fun <R> foo(x : R, block : (R) -> R) : R {
    return block(x)
}

fun box() : String {
    val result = foo(1) { x -> x + 1 }
    assertEquals(2, result)

    return "OK"
}

// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val result = (1..5).fold(0) { x, y -> x + y }

    assertEquals(15, result)

    return "OK"
}

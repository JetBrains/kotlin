// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    var result = 0
    if (1 == 1) {
        val x: Int? = 1
        result += x!!
    }

    assertEquals(1, result)
    return "OK"
}

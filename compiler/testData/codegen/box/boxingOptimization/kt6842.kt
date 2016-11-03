// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val x = (10L..50).map { it * 40L }
    assertEquals(400L, x.first())
    return "OK"
}

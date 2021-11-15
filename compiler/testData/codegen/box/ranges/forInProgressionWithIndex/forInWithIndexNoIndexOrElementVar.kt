// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    var count = 0
    for ((_, _) in (4..7).withIndex()) {
        count++
    }
    assertEquals(4, count)

    return "OK"
}
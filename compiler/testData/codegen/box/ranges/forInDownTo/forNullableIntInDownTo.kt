// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    var sum = 0
    for (i: Int? in 4 downTo 1) {
        sum = sum * 10 + (i ?: 0)
    }
    assertEquals(4321, sum)

    return "OK"
}
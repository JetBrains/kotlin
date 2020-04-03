// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    var sum = 0L
    for (i in 4L downTo 1L) {
        sum = sum * 10L + i
    }
    assertEquals(4321L, sum)

    return "OK"
}
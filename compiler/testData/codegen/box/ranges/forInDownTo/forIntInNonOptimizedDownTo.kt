// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    var sum = 0
    val dt = 4 downTo 1
    for (i in dt) {
        sum = sum * 10 + i
    }
    assertEquals(4321, sum)

    return "OK"
}
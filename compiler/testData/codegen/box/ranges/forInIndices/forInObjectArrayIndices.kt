// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    var sum = 0
    for (i in arrayOf("", "", "", "").indices) {
        sum += i
    }
    assertEquals(6, sum)

    return "OK"
}
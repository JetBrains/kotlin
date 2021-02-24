// WITH_RUNTIME

import kotlin.test.assertEquals

fun low() = 4
fun high() = 1

fun box(): String {
    var sum = 0
    for (i in low() downTo high()) {
        sum = sum * 10 + i
    }
    assertEquals(4321, sum)

    return "OK"
}
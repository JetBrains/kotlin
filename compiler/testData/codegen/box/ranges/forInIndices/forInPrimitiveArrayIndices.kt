// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    var sum = 0
    for (i in intArrayOf(0, 0, 0, 0).indices) {
        sum += i
    }
    assertEquals(6, sum)

    return "OK"
}
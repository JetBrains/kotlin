// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    testIntInIntUntilSmartcastInt()
    return "OK"
}

private fun testIntInIntUntilSmartcastInt() {
    var sum = 0

    val a: Any = 5
    if (a is Int) {
        for (i: Int in 1 until a) {
            sum = sum * 10 + i
        }
    }

    assertEquals(1234, sum)
}
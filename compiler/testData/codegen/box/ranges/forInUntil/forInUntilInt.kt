// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    testIntInIntUntilInt()
    testNullableIntInIntUntilInt()
    return "OK"
}

private fun testIntInIntUntilInt() {
    var sum = 0
    for (i in 1 until 5) {
        sum = sum * 10 + i
    }
    assertEquals(1234, sum)
}

private fun testNullableIntInIntUntilInt() {
    var sum = 0
    for (i: Int? in 1 until 5) {
        sum = sum * 10 + (i?.toInt() ?: break)
    }
    assertEquals(1234, sum)
}

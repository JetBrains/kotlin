// WITH_RUNTIME

import kotlin.test.assertEquals

fun testLongInLongUntilLong() {
    var sum = 0
    for (i in 1L until 5L) {
        sum = sum * 10 + i.toInt()
    }
    assertEquals(1234, sum)
}

fun testLongInLongUntilInt() {
    var sum = 0
    for (i in 1L until 5.toInt()) {
        sum = sum * 10 + i.toInt()
    }
    assertEquals(1234, sum)
}

fun testLongInIntUntilLong() {
    var sum = 0
    for (i in 1.toInt() until 5L) {
        sum = sum * 10 + i.toInt()
    }
    assertEquals(1234, sum)
}

fun testNullableLongInIntUntilLong() {
    var sum = 0
    for (i: Long? in 1.toInt() until 5L) {
        sum = sum * 10 + (i?.toInt() ?: break)
    }
    assertEquals(1234, sum)
}

fun box(): String {
    testLongInLongUntilLong()
    testLongInIntUntilLong()
    testLongInLongUntilInt()
    testNullableLongInIntUntilLong()
    return "OK"
}
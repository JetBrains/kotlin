// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.test.assertEquals

fun Int.digitsUpto(end: Int): Int {
    var sum = 0
    for (i in rangeTo(end)) {
        sum = sum*10 + i
    }
    return sum
}

fun box(): String {
    assertEquals(1234, 1.digitsUpto(4))
    return "OK"
}


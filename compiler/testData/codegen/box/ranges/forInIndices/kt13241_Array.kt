// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun test(x: Any): Int {
    var sum = 0
    if (x is IntArray) {
        for (i in x.indices) {
            sum = sum * 10 + i
        }
    }
    return sum
}

fun box(): String {
    // Only run this test if primitive array `is` checks work (KT-17137)
    if ((intArrayOf() as Any) is Array<*>) return "OK"

    assertEquals(123, test(intArrayOf(0, 0, 0, 0)))
    return "OK"
}
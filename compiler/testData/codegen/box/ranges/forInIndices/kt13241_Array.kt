// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

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
    assertEquals(123, test(intArrayOf(0, 0, 0, 0)))
    return "OK"
}
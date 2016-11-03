// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

fun suppressBoxingOptimization(ni: Int?) {}

fun Int.digitsUpto(end: Int): Int {
    var sum = 0
    for (i: Int? in rangeTo(end)) {
        suppressBoxingOptimization(i)
        sum = sum*10 + i!!
    }
    return sum
}

fun box(): String {
    assertEquals(1234, 1.digitsUpto(4))
    return "OK"
}


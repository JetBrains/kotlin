// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

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
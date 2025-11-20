// WITH_STDLIB

import kotlin.test.*

var b = true

fun box(): String {
    var x = 1
    if (b) {
        var x = 2
    }
    assertEquals(1, x)
    return "OK"
}

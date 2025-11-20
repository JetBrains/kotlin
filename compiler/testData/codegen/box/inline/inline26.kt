// WITH_STDLIB

import kotlin.test.*

inline fun call(block1: () -> Unit, noinline block2: () -> Int): Int {
    block1()
    return block2()
}

fun box(): String {
    var x = 5
    assertEquals(5, call({ x = 7 }, x::toInt))

    return "OK"
}

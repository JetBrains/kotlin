// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val b = 'b'
    val c = 'c'
    assertEquals('c', b + 1)
    assertEquals('a', b - 1)
    assertEquals(1, c - b)

    val list = listOf('b', 'a')
    assertEquals('c', list[0] + 1)
    assertEquals('a', list[0] - 1)
    assertEquals(1, list[0] - list[1])
    assertEquals(1, list[0] - 'a')
    assertEquals(1, 'b' - list[1])

    return "OK"
}
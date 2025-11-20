// WITH_STDLIB

import kotlin.test.*

fun sum(a:Int): Int = a + 33

fun box(): String {
    assertEquals(35, sum(2))
    return "OK"
}

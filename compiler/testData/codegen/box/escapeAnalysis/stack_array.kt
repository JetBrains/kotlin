// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val array = IntArray(2)
    array[0] = 1
    array[1] = 2
    val check = array is IntArray
    assertTrue(check)
    assertEquals(3, array[0] + array[1])

    return "OK"
}

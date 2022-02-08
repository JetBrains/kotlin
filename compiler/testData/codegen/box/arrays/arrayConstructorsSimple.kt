// WITH_STDLIB

import kotlin.test.assertEquals

fun simpleIntArray(): Array<Int> = Array(3) { it }
fun simpleDoubleArray(): Array<Double> = Array(3) { it.toDouble() + 0.1 }
fun simpleStringArray(): Array<String> = Array(3) { it.toString() }

fun box(): String {
    val ia = simpleIntArray()
    assertEquals(0, ia[0])
    assertEquals(1, ia[1])
    assertEquals(2, ia[2])

    val da = simpleDoubleArray()
    assertEquals(0.1, da[0])
    assertEquals(1.1, da[1])
    assertEquals(2.1, da[2])

    val sa = simpleStringArray()
    assertEquals("0", sa[0])
    assertEquals("1", sa[1])
    assertEquals("2", sa[2])

    return "OK"
}

// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val indexList = mutableListOf<Int>()
    val valueList = mutableListOf<Int>()
    for ((i, v) in (7 downTo 4).withIndex()) {
        indexList += i
        valueList += v
    }
    assertEquals(listOf(0, 1, 2, 3), indexList)
    assertEquals(listOf(7, 6, 5, 4), valueList)

    return "OK"
}
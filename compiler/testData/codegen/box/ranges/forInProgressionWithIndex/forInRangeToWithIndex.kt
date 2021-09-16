// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val indexList = mutableListOf<Int>()
    val valueList = mutableListOf<Int>()
    for ((i, v) in (4..7).withIndex()) {
        indexList += i
        valueList += v
    }
    assertEquals(listOf(0, 1, 2, 3), indexList)
    assertEquals(listOf(4, 5, 6, 7), valueList)

    return "OK"
}
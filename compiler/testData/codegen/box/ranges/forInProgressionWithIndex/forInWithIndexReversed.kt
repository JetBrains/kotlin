// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val indexList = mutableListOf<Int>()
    val valueList = mutableListOf<Int>()
    for ((i, v) in (4..7).withIndex().reversed()) {
        indexList += i
        valueList += v
    }
    assertEquals(listOf(3, 2, 1, 0), indexList)
    assertEquals(listOf(7, 6, 5, 4), valueList)

    return "OK"
}

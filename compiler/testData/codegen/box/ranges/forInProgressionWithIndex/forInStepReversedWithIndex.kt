// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val indexList = mutableListOf<Int>()
    val valueList = mutableListOf<Int>()
    for ((i, v) in (4..11 step 2).reversed().withIndex()) {
        indexList += i
        valueList += v
    }
    assertEquals(listOf(0, 1, 2, 3), indexList)
    assertEquals(listOf(10, 8, 6, 4), valueList)

    return "OK"
}
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val indexList = mutableListOf<Int>()
    val valueList = mutableListOf<Int>()
    val valueAndIndexList = mutableListOf<Int>()
    for ((i, v) in (4..7).withIndex()) {
        val (v2, i2) = Pair(v, i)
        indexList += i
        valueList += v
        valueAndIndexList += v2
        valueAndIndexList += i2
    }
    assertEquals(listOf(0, 1, 2, 3), indexList)
    assertEquals(listOf(4, 5, 6, 7), valueList)
    assertEquals(listOf(4, 0, 5, 1, 6, 2, 7, 3), valueAndIndexList)

    return "OK"
}
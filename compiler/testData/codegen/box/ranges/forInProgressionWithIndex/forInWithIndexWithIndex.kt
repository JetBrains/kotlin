// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val outerIndexList = mutableListOf<Int>()
    val innerIndexList = mutableListOf<Int>()
    val valueList = mutableListOf<Int>()
    for ((outer, iv) in (4..7).withIndex().withIndex()) {
        outerIndexList += outer
        val (inner, v) = iv
        innerIndexList += inner
        valueList += v
    }
    assertEquals(listOf(0, 1, 2, 3), outerIndexList)
    assertEquals(listOf(0, 1, 2, 3), innerIndexList)
    assertEquals(listOf(4, 5, 6, 7), valueList)

    return "OK"
}
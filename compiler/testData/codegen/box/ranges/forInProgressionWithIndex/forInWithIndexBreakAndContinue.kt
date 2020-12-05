// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val indexList = mutableListOf<Int>()
    val valueList = mutableListOf<Int>()
    for ((i, v) in (4..11 step 2).withIndex()) {
        if (i == 0) continue
        if (i == 3) break
        indexList += i
        valueList += v
    }
    assertEquals(listOf(1, 2), indexList)
    assertEquals(listOf(6, 8), valueList)

    return "OK"
}
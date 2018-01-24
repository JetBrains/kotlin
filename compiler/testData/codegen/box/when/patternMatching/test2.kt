// WITH_RUNTIME

import kotlin.test.assertEquals

fun matcher(p: Any?) = when (p) {
    is String -> listOf(0)
    is Pair(val a, Pair(val b, val c)) -> listOf(1, a, b, c)
    else -> listOf(2)
}

fun box() : String {
    val p: Any = Pair(1, Pair(2, 3))
    assertEquals(matcher(p), listOf(1, 1, 2, 3))
    return "OK"
}

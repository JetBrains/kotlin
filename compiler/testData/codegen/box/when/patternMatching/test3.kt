// WITH_RUNTIME

import kotlin.test.assertEquals

fun matcher(value: Any?) = when (value) {
    match Pair<*, *>(a: Pair<*, *> @ Pair<*, *>(b: Int, _), :Int) -> listOf(0, a, b)
    match x -> listOf(1, x)
    else -> throw java.lang.IllegalStateException("Unexpected else")
}

fun box() : String {
    assertEquals(matcher(Pair(Pair(2, 3), 1)), listOf(0, Pair(2, 3), 2))
    assertEquals(matcher(Pair(Pair("1", "2"), 1)), listOf(1, Pair(Pair("1", "2"), 1)))
    return "OK"
}
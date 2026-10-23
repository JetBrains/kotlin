// ISSUE: KT-90176
// WITH_STDLIB

fun <T : Comparable<T>> firstOf(list: List<*>): T =
    @Suppress("UNCHECKED_CAST") (list[0] as T)

fun box(): String {
    val x: Any = firstOf(listOf(1, 2))

    return when (x) {
        1 -> "OK"
        else -> "FAIL: $x"
    }
}

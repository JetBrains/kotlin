// WITH_STDLIB

fun box(): String {
    val predicate: (Int) -> Boolean = { x -> x != 2 }
    val list = mutableListOf<Int>()
    sequenceOf(1, 2, 3).filterTo(list, predicate)
    val expected = listOf(1, 3)
    var index = 0
    for (el in list) {
        if (expected[index++] != el) return "failed: expected ${expected[index - 1]}, but got $el"
    }
    return "OK"
}

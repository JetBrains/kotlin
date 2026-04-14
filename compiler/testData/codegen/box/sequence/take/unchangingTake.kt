// WITH_STDLIB

fun box(): String {
    val seq = generateSequence(1) { if (it < 7) it + 1 else null }.take(10).map { it * 2 }.take(8).filter { it < 14 }.take(7).take(6)
    val expected = listOf(2, 4, 6, 8, 10, 12)
    var index = 0
    for (i in seq) {
        if (expected[index++] != i) return "failed: expected ${expected[index - 1]}, got $i"
    }
    return "OK"
}

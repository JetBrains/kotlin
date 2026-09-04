// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.dropWhile
fun box(): String {
    val seq = generateSequence(16) { if (it != 1) it / 2 else null }.map { it + 1 }
    val expected = listOf(5, 3, 2)
    var index = 0
    for (el in seq.dropWhile { it > 8 }) {
        if (expected[index++] != el) return "failed: expected ${expected[index - 1]}, but got $el"
    }
    if (index != expected.size) return "failed: expected array size to be ${expected.size}, but got $index"
    return "OK"
}

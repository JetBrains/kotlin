// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.dropWhile
fun box(): String {
    val seq = sequenceOf(16, 8, 4, 2, 1, 9).map { it + 1 }
    val expected = listOf(5, 3, 2, 10)
    var index = 0
    for (el in seq.dropWhile { it > 8 }) {
        if (expected[index++] != el) return "failed: expected ${expected[index - 1]}, but got $el"
    }
    if (index != expected.size) return "failed: expected array size to be ${expected.size}, but got $index"
    return "OK"
}

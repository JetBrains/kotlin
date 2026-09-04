// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/Sequence.iterator

fun box(): String {
    val seq = generateSequence(16) { if (it != 1) it / 2 else null }.map { it + 1 }
    val expected = listOf(17, 9, 5)
    var index = 0
    for (el in seq.takeWhile { it > 4 }) {
        if (expected[index++] != el) return "failed: expected ${expected[index - 1]}, but got $el"
    }
    if (index != expected.size) return "failed: expected array size to be ${expected.size}, but got $index"
    return "OK"
}

// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.forEach
fun box(): String {
    val expected = listOf(1, 2, 3)
    var index = 0
    var result = "OK"
    sequence {
        yieldAll(listOf(1, 2, 3).iterator())
    }.forEach { if (expected[index++] != it) result = "failed: expected ${expected[index - 1]}, actual $it" }
    val seq = sequence {
        yieldAll(arrayOf(1, 2, 3, 4, 5).iterator())
    }.map { it * 2 }
    val expected2 = listOf(2, 4, 6, 8, 10)
    index = 0
    for (el in seq)
        if (expected2[index++] != el) result = "failed: expected ${expected2[index - 1]}, actual $el"
    return result
}

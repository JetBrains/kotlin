// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 2 iterator
fun box(): String {
    var k = 1
    val seq = generateSequence(k, { if (it < 4) it + 1 else null })
    k = 2
    val expected = listOf(1, 2, 3, 4)
    var index = 0
    for (item in seq) {
        if (item != expected[index++]) return "failed: sequence yielded: $item, while the expected was: ${expected[index - 1]} at index: ${index - 1}"
    }
    k = 1
    val seq2 = generateSequence(2 * k - 1, { if (it < 4) it + 1 else null })
    k = 2
    index = 0
    for (item in seq2) {
        if (item != expected[index++]) return "failed: sequence2 yielded: $item, while the expected was: ${expected[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}

// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 iterator
fun box(): String {
    val list = listOf(1, 2, 3)
    val seq = list.asSequence().map { it * 2 }
    val expected = listOf(2, 4, 6)
    var index = 0
    for (item in seq) {
        if (item != expected[index++]) return "failed: sequence yielded: $item, while the expected was: ${expected[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}

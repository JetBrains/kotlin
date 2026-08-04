// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 asSequence
fun box(): String {
    val list = listOf(1, 2, 3)
    val expected = listOf(2, 4, 6)
    var index = 0
    for (item in list.asSequence().map { it * 2 }) {
        if (item != expected[index++]) return "failed: expected: ${expected[index - 1]}, but got $item"
    }
    return "OK"
}

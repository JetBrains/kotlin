// WITH_STDLIB

// CHECK_BYTECODE_TEXT
fun box(): String {
    val sequence = sequenceOf(1, 2, 3).map { it - 1 }.filter { it > 0 }.map { it + 3 }
    val expected = listOf(4, 5)
    var index = 0
    for (item in sequence) {
        if (expected[index++] != item) return "failed on first iteration: expected=${expected[index - 1]}, actual=$item"
    }
    index = 0
    for (item in sequence) {
        if (expected[index++] != item) return "failed on second iteration: expected=${expected[index - 1]}, actual=$item"
    }
    return "OK"
}

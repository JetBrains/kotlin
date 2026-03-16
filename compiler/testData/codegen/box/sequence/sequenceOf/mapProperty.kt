// WITH_STDLIB

// CHECK_BYTECODE_TEXT
fun box(): String {
    val seq = sequenceOf("abc", "bc", "a")
    val list = listOf(3, 2, 1)
    var index = 0
    for (item in seq.map { it.length }) {
        if (list[index++] != item) return "failed: sequence yielded $item, while expected: ${list[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}

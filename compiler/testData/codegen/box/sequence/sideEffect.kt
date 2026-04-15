// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 1 TABLESWITCH
fun box(): String {
    var factor = 2
    val seq = sequenceOf(1, 2, 3).map { it * factor }

    val expected = listOf(2, 0, 0)
    var index = 0
    for (x in seq) {
        if (expected[index++] != x) return "failed: expected ${expected[index - 1]}, got $x"
        factor = 0
    }
    return "OK"
}

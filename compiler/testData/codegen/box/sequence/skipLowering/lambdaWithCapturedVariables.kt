// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 iterator
fun box(): String {
    var seq = sequenceOf(1, 2, 3)
    run {
        var k = 7
        seq = seq.map { it * k }
    }
    val list = listOf(7, 14, 21)
    var index = 0
    for (item in seq) {
        if (list[index++] != item) return "failed: sequence yielded: $item, while the expected was: ${list[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}
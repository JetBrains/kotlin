// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 2 iterator
fun box(): String {
    var k = 1
    val seq = sequenceOf(k)
    k = 2
    val list = listOf(1)
    var index = 0
    for (item in seq) {
        if (list[index++] != item) return "failed: expected ${list[index - 1]}, but got $item"
    }
    k = 0
    val seq2 = sequenceOf(1).take(k)
    k = 1
    for (item in seq2) {
        return "failed: modified $k after initialization of seq2"
    }
    return "OK"
}

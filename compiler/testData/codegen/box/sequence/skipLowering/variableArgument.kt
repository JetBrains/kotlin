// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 iterator
fun box(): String {
    var k = 1
    val seq = sequenceOf(k)
    k = 2
    val list = listOf(1)
    var index = 0
    for (item in seq) {
        if (list[index++] != item) return "failed: expected ${list[index - 1]}, but got $item"
    }
    return "OK"
}
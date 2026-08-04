// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 iterator
fun box(): String {
    var seq = sequenceOf(1, 2, 3)
    if (false) {
        seq = sequenceOf(4, 5, 6)
    }
    val list = listOf(1, 2, 3)
    var index = 0
    for (item in seq) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]}, but got $item"
    }
    return "OK"
}
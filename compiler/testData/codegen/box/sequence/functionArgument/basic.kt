// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 iterator
fun negate(sequence: Sequence<Int>): String {
    val list = listOf(1, 2, 3)
    val seq2 = sequence.map { it * -1 }
    var index = 0
    for (item in seq2) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]} but got $item"
    }
    return "OK"
}

fun box(): String {
    val seq = sequenceOf(-1, -2, -3)
    return negate(seq)
}

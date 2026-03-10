// WITH_STDLIB

fun negate(sequence: Sequence<Int>): String {
    val seq2 = sequence.map { it * 1 }
    val list = listOf(1, 2, 3)
    var index = 0
    for (item in sequence.map { it * -1 }) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]} but got $item"
    }
    index = 0
    for (item in seq2.map { it * -1 }) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]} but got $item"
    }
    return "OK"
}

fun box(): String {
    val seq = sequenceOf(-1, -2, -3)
    return negate(seq)
}
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
fun test(seq: Sequence<Int>): String? {
    val seq1 = seq.map { it * 4 }
    val seq2 = seq1.map { it / 2 }
    val seq3 = seq2.map { it * 3 }
    val seq4 = seq3.map { it + 1 }
    var index = 0
    val list = listOf(31, 37, 43)
    for (item in seq4) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]}, but actual is ${item}"
    }
    return null
}

fun box(): String {
    val seq = sequenceOf(5, 6, 7)
    if (test(seq) != null) return test(seq)!!
    val seq1 = seq.map { it * 4 }
    val seq2 = seq1.map { it / 2 }
    val seq3 = seq2.map { it * 3 }
    val seq4 = seq3.map { it + 1 }
    val list = listOf(31, 37, 43)
    var index = 0
    for (item in seq4) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]}, but actual is ${item}"
    }
    index = 0
    for (item in seq4.map { it + 1 }) {
        if (item != list[index++] + 1) return "failed: expected ${list[index - 1] + 1}, but actual is ${item}"
    }
    return "OK"
}

// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
fun box(): String {
    val seq1 = sequenceOf(1, 2, 3)
    val seq2 = sequenceOf(4, 5, 6)
    var result = ""
    for (i in seq1) {
        for (j in seq2) {
            result += "$i$j"
        }
    }
    val expected = "141516242526343536"
    if (result != expected) return "fail: expected $expected, got $result"
    return "OK"
}

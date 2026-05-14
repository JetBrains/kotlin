// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 first
fun box(): String {
    val seq = sequenceOf(4, 2, 3, 4, 5).filter { it < 4 }
    val x = seq.first()
    if (x != 2) return "failed expected 2, but got $x"

    val seq2 = generateSequence { 1 }.map { it * 2 }
    val y = seq2.first()
    if (y != 2) return "failed expected 2, but got $y"

    val seq3 = sequenceOf(null)
    val z = seq3.first()
    if (z != null) return "failed expected null, but got $z"

    val seq4 = generateSequence { null }
    try {
        seq4.first()
    } catch (e: NoSuchElementException) {
        return "OK"
    }
    return "failed: no exception thrown"
}

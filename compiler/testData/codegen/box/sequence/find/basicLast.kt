// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 last
fun box(): String {
    val seq = sequenceOf(4, 2, 3, 4, 5).filter { it < 4 }
    val x = seq.last()
    if (x != 3) return "failed expected 3, but got $x"

    val seq2 = generateSequence(1) { if (it < 5) it + 1 else null }.map { it * 2 }
    val y = seq2.last { it < 6 }
    if (y != 4) return "failed expected 4, but got $y"

    val seq3 = sequenceOf(null)
    val z = seq3.last()
    if (z != null) return "failed expected null, but got $z"

    val seq4 = generateSequence { null }
    try {
        seq4.last()
    } catch (e: NoSuchElementException) {
        return "OK"
    }
    return "failed: no exception thrown"
}

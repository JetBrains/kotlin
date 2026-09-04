// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 firstOrNull
// 0 first
fun box(): String {
    val seq = sequenceOf(4, 2, 3, 4, 5).filter { it < 4 }
    val x: Int = seq.first()
    if (x != 2) return "failed expected 2, but got $x"

    val y = generateSequence(1) { it + 1 }.map { it * 2 }.firstOrNull { it > 4 }
    if (y != 6) return "failed expected 6, but got $y"

    val seq3 = sequenceOf<Int>()
    val z = seq3.firstOrNull<Int>()
    if (z != null) return "failed expected null, but got $z"

    val seq4 = generateSequence { null }
    try {
        seq4.first()
    } catch (e: NoSuchElementException) {
        val result = sequenceOf(1, 2, 3, 4).firstNotNullOf { if (it == 3) "OK" else null }
        val nul = sequenceOf(1, 2, 3, 4).firstNotNullOfOrNull { if (it == 7) "OK" else null }
        if (nul != null) return "failed: FirstNotNullOfOrNull return non-null value $nul"
        return result
    }
    return "failed: no exception thrown"
}

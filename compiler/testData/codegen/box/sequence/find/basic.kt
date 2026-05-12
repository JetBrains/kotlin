// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(1, 2, 3).map { it * 2 }
    val x = seq.find { it % 4 == 0 }
    if (x != 4) return "failed: find returned $x"

    val seq2 = listOf(1, 2, 3).asSequence().map { it - 1 }
    val y = seq2.find { it > 2 }
    if (y != null) return "failed: find returned $y"
    return "OK"
}

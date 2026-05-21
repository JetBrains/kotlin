// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 find
// 0 findLast
fun box(): String {
    val seq = sequenceOf(1, 2, 3).map { it * 2 }
    val x = seq.find { it % 4 == 0 }
    if (x != 4) return "failed: Find returned $x"

    val y = listOf(1, 2, 3).asSequence().map { it - 1 }.find { it > 2 }
    if (y != null) return "failed: Find returned $y"

    val seq3 = generateSequence(1) { if (it < 5) it + 1 else null }
    val z = seq3.findLast { it % 3 == 0 }
    if (z != 3) return "failed: FindLast returned $z"
    return "OK"
}

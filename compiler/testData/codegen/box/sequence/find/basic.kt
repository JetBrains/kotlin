// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 findLast
fun box(): String {
    val seq = sequenceOf(1, 2, 3).map { it * 2 }
    val x = seq.find { it % 4 == 0 }
    if (x != 4) return "failed: first find returned $x"

    val y = listOf(1, 2, 3).asSequence().map { it - 1 }.find { it > 2 }
    if (y != null) return "failed: second find returned $y"

    val seq3 = generateSequence(1) { if (it < 5) it + 1 else null }
    val z = seq3.findLast { it % 3 == 0 }
    if (z != 3) return "failed: FindLast returned $z"

    var yieldCount = 0
    sequence {
        yieldCount++
        yield(1)
        yieldCount++
        yield(2)
        yieldCount++
        yield(3)
    }.find { it == 2 }
    if (yieldCount != 2) return "failed: expected 2 yields, but got $yieldCount"
    return "OK"
}

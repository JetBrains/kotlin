// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.generateSequence
fun box(): String {
    var seed = 1
    val fused = generateSequence(seed) { if (it < 3) it + 1 else null }.toList()
    if (fused != listOf(1, 2, 3)) return "Fail: $fused"
    return "OK"
}

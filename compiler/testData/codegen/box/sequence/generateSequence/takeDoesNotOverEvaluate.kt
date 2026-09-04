// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val r = generateSequence(1) { if (it == 3) error("boom") else it + 1 }.take(3).toList()
    return if (r == listOf(1, 2, 3)) "OK" else "Fail: $r"
}

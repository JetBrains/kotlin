// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val r = sequence { yieldAll(sequenceOf(1, 2, 3).map { it * 10 }.filter { it > 10 }) }.toList()
    return if (r == listOf(20, 30)) "OK" else "Fail: $r"
}

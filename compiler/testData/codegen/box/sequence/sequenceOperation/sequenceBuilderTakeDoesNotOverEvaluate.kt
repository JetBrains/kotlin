// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val r = sequence { yield(1); yield(2); error("boom") }.take(2).toList()
    return if (r == listOf(1, 2)) "OK" else "Fail: $r"
}

// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val r = sequence { yieldAll(listOf(1, 2)); yield(3) }.first()
    return if (r == 1) "OK" else "Fail: $r"
}

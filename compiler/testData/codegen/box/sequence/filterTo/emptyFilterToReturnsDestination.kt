// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val dest = mutableListOf(42)
    val r = emptySequence<Int>().filterTo(dest) { true }
    return if (r === dest) "OK" else "Fail: $r"
}

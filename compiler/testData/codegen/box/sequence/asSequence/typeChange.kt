// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// the declaration of the sequence variable is kept for the debugger, so its stdlib calls remain although its use is fused
// 1 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/Sequence.iterator

fun box(): String {
    val list = listOf(1)
    val seq = list.asSequence().map { if (it == 1) "OK" else "FAIL" }
    for (item in seq) {
        return item
    }
    return "SKIPPED BODY"
}

// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// the declaration of the sequence variable is kept for the debugger, so its stdlib calls remain although its use is fused
// 1 kotlin/sequences/SequencesKt.map
// the lambda body is copied into the fused loop
// 2 LDC "FAIL"

fun test(seq: Sequence<Int>): String {
    val seq2 = seq.map { if (it == 1) "OK" else "FAIL" }
    for (item in seq2) {
        return item
    }
    return "SKIPPED BODY"
}

fun box(): String = test(sequenceOf(1))
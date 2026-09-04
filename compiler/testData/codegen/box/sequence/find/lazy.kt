// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// the declaration of the sequence variable is kept for the debugger, so its stdlib calls remain although its use is fused
// 1 kotlin/sequences/SequencesKt.generateSequence
// 0 kotlin/sequences/Sequence.iterator

fun box(): String {
    val seq = generateSequence(3) { it - 1 }
    val x = seq.find { 1 / it == 1 }
    if (x != 1) return "failed: result of find was $x"
    return "OK"
}

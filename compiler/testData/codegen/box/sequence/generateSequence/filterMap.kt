// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// the declaration of the sequence variable is kept for the debugger, so its stdlib calls remain although its use is fused
// 1 kotlin/sequences/SequencesKt.generateSequence
// 1 kotlin/sequences/SequencesKt.map
// 1 kotlin/sequences/SequencesKt.filter
// 0 kotlin/sequences/Sequence.iterator

fun box(): String {
    val seq = generateSequence(1, { if (it < 8) it * 2 else null }).map { it * 2 }.filter { it != 4 }
    val list = listOf(3, 17)
    var index = 0
    for (item in seq.filter { it != 8 }.map { it + 1 }) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]}, but got $item"
    }
    return "OK"
}
// WITH_STDLIB

fun test(seq: Sequence<Int>): Boolean {
    val seq2 = seq.map { it + 1 }.filter { it % 2 == 0 }.map { it * 2 }
    for (item in seq2) {
        return true
    }
    return false
}

fun test2(): Boolean {
    val seq = sequenceOf(1, 2, 3)
    val seq2 = seq.map { it + 1 }.filter { it % 2 == 0 }.map { it * 2 }
    for (item in seq2) {
        return true
    }
    return false
}

fun box(): String {
    val seq = sequenceOf(1, 2, 3)
    if (!test(seq)) return "failed: sequence skipped loop body when lowering with function argument"
    if (!test2()) return "failed: sequence skipped loop body when lowering with sequenceOf"
    return "OK"
}
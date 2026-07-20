// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
fun box(): String {
    val seedFunction = { 12 }
    val seq = generateSequence(seedFunction).map { it + 1 }
    for (el in seq) {
        if (el != 13) return "failed: expected 12, got $el"
        return "OK"
    }
    return "failed: sequence iteration was skipped"
}

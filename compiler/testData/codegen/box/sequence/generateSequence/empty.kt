// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
fun box(): String {
    val seq = generateSequence(null) { it }
    for (item in seq) {
        return "failed: seq should be empty"
    }
    for (item in seq.map { it }) {
        return "failed: seq.map { it } should be empty"
    }
    val seq2 = generateSequence({ null }, { it })
    for (item in seq2) {
        return "failed: seq2 should be empty"
    }
    val seq3 = generateSequence({ null }, { it })
    for (item in seq3.map { it }) {
        return "failed: seq3.map { it } should be empty"
    }
    val seq4 = generateSequence({ null })
    for (item in seq4) {
        return "failed: seq4 should be empty"
    }
    val seq5 = generateSequence({ null })
    for (item in seq5.map { it }) {
        return "failed: seq5.map { it } should be empty"
    }
    return "OK"
}
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 iterator
// 0 LOOKUPSWITCH
// 1 TABLESWITCH
fun box(): String {
    val seq = sequenceOf(0).map { it / 0 }
    val seq2 = sequenceOf(0, 1).map { if (it == 0) "OK" else error("Not first") }
    for (el in seq2) {
        return el
    }
    return "failed: skipped iteration"
}

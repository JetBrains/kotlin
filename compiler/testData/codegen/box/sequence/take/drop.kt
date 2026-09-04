// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.drop
// 0 kotlin/sequences/SequencesKt.toList
// 0 kotlin/sequences/SequencesKt.sequence
fun box(): String {
    val expected = listOf(3, 5, 8)
    val result = sequence {
        yieldAll(listOf(1, 2, 3))
        yield(5)
        if (true) yield(8)
    }.drop(2).toList()
    if (result != expected) return "failed: expected $expected, but got $result"
    return "OK"
}

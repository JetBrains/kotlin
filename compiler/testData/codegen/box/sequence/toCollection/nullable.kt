// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.toList
fun box(): String {
    val seq: Sequence<Int?> = sequence {
        yield(1)
        yield(null)
        yield(3)
    }
    val list = seq.toList()
    if (list != listOf(1, null, 3)) return "Fail: $list"
    return "OK"
}

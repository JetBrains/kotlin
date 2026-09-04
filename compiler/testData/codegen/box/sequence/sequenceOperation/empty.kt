// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 TABLESWITCH
// 0 LOOKUPSWITCH
// 0 SequenceScope
// 0 kotlin/sequences/SequencesKt.filterTo
// 0 iterator
fun box(): String {
    val list = mutableListOf<Int>()
    sequence {
        yieldAll(listOf(2, 3))
        yieldAll(listOf<Int>())
        yieldAll(listOf(1))
    }.filterTo(list) { true }
    val expected = listOf(2, 3, 1)
    if (expected != list) return "failed: expected $expected but was $list"
    return "OK"
}

// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.toList
fun box(): String {
    val list = sequenceOf(1, 2, 3).filter { it > 1 }.map { it - 1 }.toList()
    val expected = listOf(1, 2)
    if (list != expected) return "expected $expected but was $list"
    val list2 = sequenceOf(listOf("A"), listOf("B")).toMutableList()
    if (list2[0][0] != "A") return "failed: expected \"A\", but got ${list2[0][0]}"
    return "OK"
}

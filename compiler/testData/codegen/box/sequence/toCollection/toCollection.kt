// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.toCollection
fun box(): String {
    val list = mutableListOf(1, 2)
    val result = generateSequence(1) { if (it < 5) it + 1 else null }.map { it + 2 }.toCollection(list)
    val expected = listOf(1, 2, 3, 4, 5, 6, 7)
    if (result != expected) return "failed: expected $expected as function result, but was $result"
    if (list != expected) return "failed: expected $expected under list, but was $list"
    return "OK"
}

// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.toList
fun box(): String {
    val set = sequenceOf(1, 2, 3).filter { it > 1 }.map { it - 1 }.toSet()
    val expected = setOf(1, 2)
    if (set != expected) return "expected $expected but was $set"
    return "OK"
}

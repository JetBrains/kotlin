// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 SequencesKt.last
suspend fun SequenceScope<Int>.mySequenceScope() {
    yieldAll(listOf(1, 2, 3))
    yield(4)
    yieldAll(listOf())
}

fun box(): String {
    val result = sequence(SequenceScope<Int>::mySequenceScope).last()
    if (result != 4) return "fail, got $result"
    return "OK"
}

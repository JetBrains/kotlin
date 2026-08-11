// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 kotlin/sequences/SequencesKt.sequence
suspend fun <T> SequenceScope<T>.yieldIfNotNull(t: T?) {
    if (t != null) yield(t)
}

fun box(): String {
    val result = sequence {
        yieldIfNotNull(null)
        yieldIfNotNull(1)
        yieldIfNotNull(2)
        yieldIfNotNull(null)
    }.toList()
    if (result != listOf(1, 2)) return "failed: expected ${listOf(1, 2)}, but got $result"
    return "OK"
}

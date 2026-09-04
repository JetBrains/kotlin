// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    try {
        sequenceOf(1, 2).first { it > 5 }
    } catch (e: NoSuchElementException) {
        return if (e.message == "Sequence contains no element matching the predicate.") "OK" else "Fail: ${e.message}"
    }
    return "Fail: no exception"
}

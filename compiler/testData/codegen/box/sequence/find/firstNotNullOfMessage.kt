// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    try {
        sequenceOf(1, 2).firstNotNullOf { null }
    } catch (e: NoSuchElementException) {
        return if (e.message == "No element of the sequence was transformed to a non-null value.") "OK" else "Fail: ${e.message}"
    }
    return "Fail: no exception"
}

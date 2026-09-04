// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val n = -1
    try {
        sequenceOf(1, 2).take(n).toList()
    } catch (e: IllegalArgumentException) {
        return if (e.message == "Requested element count -1 is less than zero.") "OK" else "Fail: ${e.message}"
    }
    return "Fail: no exception"
}

// TARGET_BACKEND: JVM
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val z = 0
    try {
        sequenceOf(1, 1 / z).first()
    } catch (e: ArithmeticException) {
        return "OK"
    }
    return "Fail: no exception"
}

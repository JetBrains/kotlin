// TARGET_BACKEND: JVM
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

fun box(): String {
    val notAString: Any = 42
    val zero = 0
    val log = StringBuilder()
    try {
        sequenceOf(1, 2).filter { log.append(it); true }.map((notAString as String)::get).toList()
    } catch (e: ClassCastException) {
        log.append("CCE")
    }
    try {
        emptySequence<Int>().map((10 / zero)::plus).toList()
    } catch (e: ArithmeticException) {
        log.append("AE")
    }
    val r = log.toString()
    return if (r == "CCEAE") "OK" else "Fail: $r"
}

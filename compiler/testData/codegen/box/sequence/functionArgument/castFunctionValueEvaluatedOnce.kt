// TARGET_BACKEND: JVM
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.sequenceOf
// 0 kotlin/sequences/SequencesKt.map
// 0 kotlin/sequences/SequencesKt.filter

fun box(): String {
    val notAFunction: Any = "x"
    val log = StringBuilder()
    try {
        sequenceOf(1, 2).map { log.append(it); it }.filter(notAFunction as (Int) -> Boolean).toList()
    } catch (e: ClassCastException) {
        log.append("CCE")
    }
    try {
        emptySequence<Int>().forEach(notAFunction as (Int) -> Unit)
    } catch (e: ClassCastException) {
        log.append("CCE")
    }
    val wrongArity: Any = { a: Int, b: Int -> a + b }
    try {
        generateSequence(wrongArity as () -> Int?).take(0).toList()
    } catch (e: ClassCastException) {
        log.append("CCE")
    }
    val r = log.toString()
    return if (r == "CCECCECCE") "OK" else "Fail: $r"
}

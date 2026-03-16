// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 4 iterator
fun box(): String {
    val sequence = generateSequence(1) { x: Int -> if (x < 5) x + 1 else null }
    for (item in sequence) {}
    for (item in sequence) {}
    val seq2 = generateSequence({ 1 }) { x: Int -> if (x < 3) x + 1 else null }
    for (item in seq2) {}
    for (item in seq2) {}
    try {
        val seq3 = generateSequence { null }
        // should be not lowered
        for (i in seq3) {}
        for (i in seq3) {}
    } catch (e: IllegalStateException) {
        if (e.message != "This sequence can be consumed only once.") return "Exception thrown has wrong message: ${e.message}"
        try {
            generateSequence { null }.let { seq4 ->
                // should be not lowered
                for (i in seq4) {}
                for (i in seq4) {}
            }
        } catch (e: IllegalStateException) {
            if (e.message != "This sequence can be consumed only once.") return "Exception thrown has wrong message: ${e.message}"
            val seq5 = generateSequence { null }
            // should lower, only one use
            for (i in seq5) {}
            return "OK"
        }
    }
    return "failed: generateSequence(() -> T?) should not allow reusing the sequence"
}

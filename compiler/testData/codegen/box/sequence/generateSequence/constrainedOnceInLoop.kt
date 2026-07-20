// WITH_STDLIB
fun box(): String {
    var k = 0
    val s = generateSequence { if (k < 2) k++ else null }
    var iterations = 0
    try {
        repeat(2) {
            iterations++
            for (x in s) {}
        }
    } catch (e: IllegalStateException) {
        return if (iterations == 2) "OK" else "Fail: iterations=$iterations"
    }
    return "Fail: no exception"
}

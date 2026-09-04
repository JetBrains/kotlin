// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt.take
// 0 kotlin/sequences/SequencesKt.toList

fun takeFrom(s: Sequence<Int>, n: Int): List<Int> = s.take(n).toList()
fun box(): String {
    val s = listOf(1, 2).asSequence().constrainOnce()
    s.toList()
    try {
        takeFrom(s, -1)
    } catch (e: IllegalArgumentException) {
        return "OK"
    } catch (e: Throwable) {
        return "Fail: $e"
    }
    return "Fail: no exception"
}

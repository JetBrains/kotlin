// WITH_STDLIB

fun box(): String {
    val list = listOf(1)
    val seq2 = list.asSequence().map { if (it == 1) "OK" else "FAIL" }
    for (item in seq2) {
        return item
    }
    return "SKIPPED BODY"
}

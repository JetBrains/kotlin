// WITH_STDLIB

fun box(): String {
    val list = listOf(1)
    val seq = list.asSequence().map { if (it == 1) "OK" else "FAIL" }
    for (item in seq) {
        return item
    }
    return "SKIPPED BODY"
}

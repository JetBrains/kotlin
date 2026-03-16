// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(5, 7, 9).map { it.toString() }
    val list = listOf("5", "7", "9")
    var index = 0
    for (item in seq) {
        if (list[index++] != item) return "failed: sequence yielded: $item, while the expected was: ${list[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}

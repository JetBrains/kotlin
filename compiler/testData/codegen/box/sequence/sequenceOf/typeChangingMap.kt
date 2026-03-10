// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(5, 7, 9).map { it.toString() }.map { it.toInt() + 5 }
    val list = listOf(10, 12, 14)
    var index = 0
    for (item in seq) {
        if (list[index++] != item) return "failed: sequence yielded: $item, while the expected was: ${list[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}
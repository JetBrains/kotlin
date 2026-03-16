// WITH_STDLIB

fun box(): String {
    val array = arrayOf(1, 2, 3)
    val seq = array.asSequence().map { it * 2 }
    val list = listOf(2, 4, 6)
    var index = 0
    for (item in seq) {
        if (item != list[index++]) return "failed: sequence yielded: $item, while the expected was: ${list[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}

// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(1, 2, 3)
    val list = listOf(1, 2, 3)
    var index = 0
    for (item in seq) {
        if (item != list[index++]) return "failed: sequence yielded: $item, while the expected was: ${list[index - 1]} at index: ${index - 1}"
    }
    index = 0
    for (item in sequenceOf(1, 2, 3)) {
        if (item != list[index++]) return "failed: sequence yielded: $item, while the expected was: ${list[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}
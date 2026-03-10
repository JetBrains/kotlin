// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(1, 2, 3).map { it * 3 }.map { it * 2 }
    val list = listOf(6, 12, 18)
    var index = 0
    val list2 = mutableListOf<Int>()
    for (item in seq) {
        list2.add(item)
    }
    for (item in seq) {
        if (list[index] != item) return "failed: sequence yielded: $item, while the expected was: ${list[index]} at index: ${index}"
        if (list2[index++] != item) return "failed: sequence yielded: $item, while the expected was: ${list2[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}
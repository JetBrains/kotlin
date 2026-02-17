// WITH_STDLIB

fun test(x: Int): Int {
    return x * 2
}

fun box(): String {
    val seq = sequenceOf(1, 2, 3).map(::test)
    val list = listOf(2, 4, 6)
    var index = 0
    for (item in seq) {
        if (list[index++] != item) return "failed: sequence yielded: $item, while the expected was: ${list[index - 1]} at index: ${index - 1}"
    }
    return "OK"
}
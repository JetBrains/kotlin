// WITH_STDLIB

fun box(): String {
    var seq = sequenceOf(1, 2, 3)
    seq = sequenceOf(4, 5, 6)
    val list = listOf(4, 5, 6)
    var index = 0
    for (item in seq) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]}, but got $item"
    }
    return "OK"
}
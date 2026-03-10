// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(1, 2, 3, 4).map { it * 2 }.filter { it % 4 == 0 }.map { it / 2 }.filter { it != 2 }.map { it - 3 }
    val list = listOf(1)
    var index = 0
    for (item in seq) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]}, but got $item"
    }
    return "OK"
}
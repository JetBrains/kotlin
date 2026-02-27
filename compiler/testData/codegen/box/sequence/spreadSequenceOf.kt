// WITH_STDLIB

fun box(): String {
    var array = arrayOf(1, 2, 3)
    val seq = sequenceOf(*array).map { it * 2 }
    val list = array.map { it * 2 }.toList()
    var index = 0
    for (item in seq) {
        if (item != list[index++]) return "failed: expected: ${list[index - 1]} but was: $item"
    }
    return "OK"
}
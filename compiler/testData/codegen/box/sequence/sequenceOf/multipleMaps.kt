// WITH_STDLIB

fun box(): String {

    val seq = sequenceOf(1, 2, 3).map { it * 2 }
    val list = listOf(3, 5, 7)
    var index = 0
    for (item in seq.map { it + 1 }) {
        if (item != list[index++]) {
            return "failed: sequence yielded: $item, while the expected was: ${list[index - 1]} at index: ${index - 1}"
        }
    }
    return "OK"
}
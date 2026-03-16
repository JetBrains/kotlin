// WITH_STDLIB

fun box(): String {
    val seq = generateSequence(1, { if (it < 8) it * 2 else null }).map { it * 2 }.filter { it != 4 }
    val list = listOf(3, 17)
    var index = 0
    for (item in seq.filter { it != 8 }.map { it + 1 }) {
        if (item != list[index++]) return "failed: expected ${list[index - 1]}, but got $item"
    }
    return "OK"
}
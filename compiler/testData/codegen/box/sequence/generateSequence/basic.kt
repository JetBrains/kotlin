// WITH_STDLIB

fun box(): String {
    val seq = generateSequence(1) { if (it > 9) null else it + 1 }
    val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    var index = 0
    for (item in seq) {
        if (list[index++] != item) return "failed: expected ${list[index - 1]}, but got $item"
    }
    val seq2 = generateSequence ({ 1 }, { x: Int -> if (x > 9) null else x + 1 })
    index = 0
    for (item in seq2) {
        if (list[index++] != item) return "failed: expected ${list[index - 1]}, but got $item"
    }
    var k = 1
    val seq3 = generateSequence { if (k > 9) null else k++ }
    index = 0
    for (item in seq3) {
        if (list[index++] != item) return "failed: expected ${list[index - 1]}, but got $item"
    }
    return "OK"
}
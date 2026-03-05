import kotlin.text.plus

// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(1, 2, 3)
    val seq2 = sequenceOf(1, 2, 3)
    val list = listOf("B1", "C1", "D1", "B2", "C2", "D2", "B3", "C3", "D3")
    var index = 0
    for (item in seq.map { it.toString() }) {
        for (item2 in seq2.map { 'A'.plus(it) }) {
            if (list[index++] != item2 + item) return "failed: expected ${list[index - 1]}, but got $item2$item"
        }
    }
    return "OK"
}
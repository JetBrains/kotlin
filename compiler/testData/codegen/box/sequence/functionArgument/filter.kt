// WITH_STDLIB
fun test(seq: Sequence<Int>): String {
    val seq2 = seq.map { it * 2 }.filter { it * it != 36 }.map { it + 1 }
    val expected = listOf(3, 5)
    var index = 0
    val list = mutableListOf<Int>()
    for (item in seq2) {
        list.add(item)
    }
    for (item in seq2) {
        if (item != expected[index]) return "failed: expected ${expected[index]}, but got $item"
        if (item != list[index++]) return "failed: expected ${list[index - 1]}, but got $item"
    }
    if (index != expected.size) return "failed: sequence has $index items, but list has ${expected.size} items"
    return "OK"
}

fun box(): String {
    return test(sequenceOf(1, 2, 3))
}

// WITH_STDLIB
fun test(seq: Sequence<Int>): String {
    val seq2 = seq.map { it * 2 }.filter { it * it != 36 }.map { it + 1 }
    val list = listOf(3, 5)
    var index = 0
    val list2 = mutableListOf<Int>()
    for (item in seq2) {
        list2.add(item)
    }
    for (item in seq2) {
        if (item != list[index]) return "failed: expected ${list[index]}, but got $item"
        if (item != list2[index++]) return "failed: expected ${list2[index - 1]}, but got $item"
    }
    if (index != list.size) return "failed: sequence has $index items, but list has ${list.size} items"
    return "OK"
}

fun box(): String {
    return test(sequenceOf(1, 2, 3))
}
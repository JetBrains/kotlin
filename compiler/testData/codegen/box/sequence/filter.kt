// WITH_STDLIB

fun box(): String {
    val seq = sequenceOf(1, 2, 3, 4).filter { it % 2 == 1 }.map { it.toString() }
    val list = listOf("1", "3")
    var index = 0
    for (item in seq) {
        if (item != list[index++]) return "failed: item '$item' does not match expected value '${list[index - 1]}'"
    }
    if (index != list.size) {
        return "failed: sequence length mismatch, sequence has length $index, but expected length is ${list.size}"
    }
    return "OK"
}
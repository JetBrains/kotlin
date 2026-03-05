// WITH_STDLIB

fun box(): String {
    val list = mutableListOf<Int>()
    val seq = sequenceOf(list).map<MutableList<Int>, Unit> { it.add(1) }
    for (item in seq) {
        item
        item
    }
    if (list.size != 1) return "failed, list size was ${list.size}, but expected 1"
    return "OK"
}
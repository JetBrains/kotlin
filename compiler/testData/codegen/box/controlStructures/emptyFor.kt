var index = 0

val iterator = object : Iterator<Int> {
    override fun hasNext() = index < 5
    override fun next() = index++
}

fun box(): String {
    for (x in 1..5);

    for (x in iterator);
    if (index != 5) return "Fail: $index"

    return "OK"
}

// IGNORE_BACKEND_FIR: JVM_IR
var index = 0

interface IterableIterator : Iterator<Int> {
    operator fun iterator(): Iterator<Int> = this
}

val iterator = object : IterableIterator {
    override fun hasNext() = index < 5
    override fun next() = index++
}

fun box(): String {
    for (x in 1..5);

    for (x in iterator);
    if (index != 5) return "Fail: $index"

    return "OK"
}

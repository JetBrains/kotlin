// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

class CountingSequence<out T>(private val s: Sequence<T>) : Sequence<T> {
    var hasNextCtr = 0
    var nextCtr = 0

    inner class CountingSequenceIterator(private val it: Iterator<T>) : Iterator<T> {
        override fun hasNext() = it.hasNext().also { hasNextCtr++ }
        override fun next() = it.next().also { nextCtr++ }
    }

    override fun iterator() = CountingSequenceIterator(s.iterator())
}

val xs = CountingSequence(listOf("a", "b", "c", "d").asSequence())

fun box(): String {
    val s = StringBuilder()

    for ((index, _) in xs.withIndex()) {
        s.append("$index;")
    }

    val ss = s.toString()
    if (ss != "0;1;2;3;") return "fail: '$ss'"
    if (xs.hasNextCtr != 5) return "hasNextCtr != 5, was: '${xs.hasNextCtr}'"
    if (xs.nextCtr != 4) return "nextCtr != 4, was: '${xs.nextCtr}'"

    return "OK"
}
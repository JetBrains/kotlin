// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

class Tagged<K, V>(val tag: K, val items: List<V>) : Sequence<V> {
    override fun iterator(): Iterator<V> = items.iterator()
}
fun sum(s: Tagged<String, Int>): Int {
    var r = 0
    for (x in s.map { it * 2 }) r += x
    return r
}
fun box(): String {
    val r = sum(Tagged("t", listOf(1, 2)))
    return if (r == 6) "OK" else "Fail: $r"
}

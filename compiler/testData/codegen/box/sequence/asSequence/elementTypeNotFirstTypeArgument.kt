// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

class Box<K, V>(val key: K, val items: List<V>) : Iterable<V> {
    override fun iterator(): Iterator<V> = items.iterator()
}
fun box(): String {
    val r = Box("key", listOf(1, 2)).asSequence().map { it + 1 }.toList()
    return if (r == listOf(2, 3)) "OK" else "Fail: $r"
}

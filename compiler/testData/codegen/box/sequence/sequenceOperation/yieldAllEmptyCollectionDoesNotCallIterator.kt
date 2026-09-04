// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

// Like in the stdlib, `yieldAll` of an empty collection does not call its `iterator()`
var iteratorCalls = 0
class Tracking(val items: List<Int>) : AbstractCollection<Int>() {
    override val size: Int get() = items.size
    override fun iterator(): Iterator<Int> {
        iteratorCalls++
        return items.iterator()
    }
}
fun box(): String {
    val empty: Iterable<Int> = Tracking(emptyList())
    val r = sequence { yieldAll(empty); yieldAll(Tracking(emptyList())); yieldAll(Tracking(listOf(1))); yield(2) }.toList()
    return if (r == listOf(1, 2) && iteratorCalls == 1) "OK" else "Fail: $r, iteratorCalls=$iteratorCalls"
}

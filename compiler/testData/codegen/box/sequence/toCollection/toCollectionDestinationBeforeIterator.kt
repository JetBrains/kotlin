// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var log = ""
class Src<T>(val items: List<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> { log += "i"; return items.iterator() }
}
fun dest(): MutableList<Int> { log += "d"; return mutableListOf() }
fun box(): String {
    val r = Src(listOf(1, 2)).asSequence().toCollection(dest())
    return if (r == listOf(1, 2) && log == "di") "OK" else "Fail: $r, $log"
}

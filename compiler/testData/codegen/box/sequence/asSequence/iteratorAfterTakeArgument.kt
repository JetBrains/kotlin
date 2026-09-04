// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var log = ""
class Src<T>(val items: List<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> { log += "i"; return items.iterator() }
}
fun n(): Int { log += "n"; return 1 }
fun box(): String {
    val r = Src(listOf(1, 2)).asSequence().take(n()).toList()
    return if (r == listOf(1) && log == "ni") "OK" else "Fail: $r, $log"
}

// WITH_STDLIB
// CHECK_BYTECODE_TEXT
inline fun mapped(n: Int, crossinline g: (Int) -> Int): List<Int> = sequence { yield(n) }.map { g(it) }.toList()
inline fun looped(n: Int, g: (Int) -> Unit) { for (x in sequence { yield(n); yield(n + 1) }) g(x) }
inline fun each(n: Int, g: (Int) -> Unit) = sequence { yield(n) }.forEach(g)
fun box(): String {
    var s = 0
    looped(2) { s += it }
    each(10) { s += it }
    val m = mapped(2) { it * 3 }
    return if (m == listOf(6) && s == 15) "OK" else "Fail: $m, $s"
}

// the `sequence` builders in inline functions are not fused, neither in their bodies nor in the copies inlined into `box`
// 6 kotlin/sequences/SequencesKt.sequence \(

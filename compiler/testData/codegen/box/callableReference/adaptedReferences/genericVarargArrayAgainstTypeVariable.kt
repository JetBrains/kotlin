// ISSUE: KT-90169

fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <E> countG(vararg xs: E): Int = xs.size
fun <N : Number> countN(vararg xs: N): Int = xs.size
fun count(vararg xs: Any?): Int = xs.size

fun box(): String {
    val arr = arrayOf("a", "b")
    val c = applyIt(arr, ::count)
    val g = applyIt(arr, ::countG)
    val n = applyIt(arrayOf(1, 2), ::countN)
    if (c != 2) return "FAIL count: $c"
    if (g != 2) return "FAIL countG: $g"
    if (n != 2) return "FAIL countN: $n"
    return "OK"
}

// ISSUE: KT-90169
// WITH_STDLIB

fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <T> make(f: (T) -> Int): (T) -> Int = f

fun <E> countG(vararg xs: E): Int = xs.size
fun count(vararg xs: Any?): Int = xs.size
fun countS(vararg xs: String): Int = xs.size
fun countCS(vararg xs: CharSequence): Int = xs.size
fun countI(vararg xs: Int): Int = xs.size

class G<E> { fun c(vararg xs: E): Int = xs.size }

fun check(name: String, actual: Int, expected: Int = 2): String? =
    if (actual != expected) "FAIL $name: $actual" else null

fun box(): String {
    val arr = arrayOf("a", "b")
    val f: (Array<String>) -> Int = ::countG
    val g: (Array<String>) -> Int = make(::countG)
    return check("count", applyIt(arr, ::count))
        ?: check("countS", applyIt(arr, ::countS))
        ?: check("countCS", applyIt(arr, ::countCS))
        ?: check("let countS", arr.let(::countS))
        ?: check("expected function type", f(arr))
        ?: check("make", g(arr))
        ?: check("G<String>", applyIt(arr, G<String>()::c))
        ?: check("G<Any?>", applyIt(arr, G<Any?>()::c))
        ?: check("spread lambda", applyIt(arr) { countG(*it) })
        ?: check("lambda", applyIt(arr) { countG(it) }, expected = 1)
        ?: check("countI", applyIt(intArrayOf(1, 2), ::countI))
        ?: check("String", applyIt("x", ::countG), expected = 1)
        ?: check("IntArray", applyIt(intArrayOf(1, 2), ::countG), expected = 1)
        ?: "OK"
}

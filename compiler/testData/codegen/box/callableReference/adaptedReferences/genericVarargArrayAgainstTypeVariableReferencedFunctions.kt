// ISSUE: KT-90169
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: 2.4
// WITH_STDLIB

fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <T, R> applyR(t: T, f: (T) -> R): R = f(t)

inline fun <reified E> countR(vararg xs: E): Int = xs.size
fun <E : Any> countNN(vararg xs: E): Int = xs.size
fun <E> countD(vararg xs: E, k: Int = 0): Int = xs.size + k
fun <E> firstG(vararg xs: E): E = xs[0]

class K { fun <E> countM(vararg xs: E): Int = xs.size }
object O { fun <E> countM(vararg xs: E): Int = xs.size }
class Box<E>(vararg val xs: E)

fun check(name: String, actual: Int, expected: Int = 2): String? =
    if (actual != expected) "FAIL $name: $actual" else null

fun box(): String {
    val arr = arrayOf("a", "b")
    val first: Any? = arr.let(::firstG)
    if (first != "a") return "FAIL firstG: $first"
    return check("listOf", arr.let(::listOf).size)
        ?: check("setOf", arr.let(::setOf).size)
        ?: check("mutableListOf", arr.let(::mutableListOf).size)
        ?: check("arrayListOf", arr.let(::arrayListOf).size)
        ?: check("mutableSetOf", arr.let(::mutableSetOf).size)
        ?: check("hashSetOf", arr.let(::hashSetOf).size)
        ?: check("sequenceOf", arr.let(::sequenceOf).count())
        ?: check("listOfNotNull", arr.let(::listOfNotNull).size)
        ?: check("arrayOf", arr.let(::arrayOf).size)
        ?: check("reified", applyIt(arr, ::countR))
        ?: check("member", applyIt(arr, K()::countM))
        ?: check("object member", applyIt(arr, O::countM))
        ?: check("constructor", applyR(arr, ::Box).xs.size)
        ?: check("E : Any", applyIt(arr, ::countNN))
        ?: check("vararg and default", applyIt(arr, ::countD))
        ?: "OK"
}

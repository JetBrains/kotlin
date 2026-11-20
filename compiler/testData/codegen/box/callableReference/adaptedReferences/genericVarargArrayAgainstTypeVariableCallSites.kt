// ISSUE: KT-90169
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: 2.4
// WITH_STDLIB

fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <T> applyRev(f: (T) -> Int, t: T): Int = f(t)
fun <T : Any> applyNN(t: T, f: (T) -> Int): Int = f(t)

fun interface F<T> { fun f(t: T): Int }
fun <T> applyF(t: T, f: F<T>): Int = f.f(t)

fun <E> countG(vararg xs: E): Int = xs.size

fun check(name: String, actual: Int, expected: Int = 2): String? =
    if (actual != expected) "FAIL $name: $actual" else null

fun box(): String {
    val arr = arrayOf("a", "b")
    val n: Array<String>? = arr
    return check("explicit type argument", applyIt<Array<String>>(arr, ::countG))
        ?: check("let", arr.let(::countG))
        ?: check("safe call let", n?.let(::countG) ?: -1)
        ?: check("with", with(arr, ::countG))
        ?: check("map", listOf(arr).map(::countG).single())
        ?: check("flatMap", listOf(arr).flatMap(::listOf).size)
        ?: check("reference before array", applyRev(::countG, arr))
        ?: check("T : Any", applyNN(arr, ::countG))
        ?: check("fun interface", applyF(arr, ::countG))
        ?: "OK"
}

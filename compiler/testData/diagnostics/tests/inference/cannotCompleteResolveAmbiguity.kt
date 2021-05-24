// FIR_IDENTICAL
package f

fun <T> g(i: Int, a: Any): List<T> {throw Exception()}
fun <T> g(a: Any, i: Int): Collection<T> {throw Exception()}

fun <T> test() {
    val c: List<T> = <!OVERLOAD_RESOLUTION_AMBIGUITY!>g<!>(1, 1)
}

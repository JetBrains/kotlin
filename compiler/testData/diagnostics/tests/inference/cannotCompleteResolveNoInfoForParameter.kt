// FIR_IDENTICAL
package f

fun <T> f(i: Int, c: Collection<T>): List<T> {throw Exception()}
fun <T> f(a: Any, l: List<T>): Collection<T> {throw Exception()}

fun <T> test(l: List<T>) {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>(1, emptyList())
}

fun <T> emptyList(): List<T> {throw Exception()}

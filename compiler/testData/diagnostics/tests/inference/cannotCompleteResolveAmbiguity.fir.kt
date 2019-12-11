// !WITH_NEW_INFERENCE
package f

fun <T> g(i: Int, a: Any): List<T> {throw Exception()}
fun <T> g(a: Any, i: Int): Collection<T> {throw Exception()}

fun <T> test() {
    val c: List<T> = <!AMBIGUITY!>g<!>(1, 1)
}

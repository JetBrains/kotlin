package f

class In<in K>

fun <T> f(i: Int, c: Collection<T>): List<T> {throw Exception()}
fun <T> f(a: Any, l: List<T>): Collection<T> {throw Exception()}

fun <T, K> g(i: Int, c: Collection<T>, vararg x: In<K>): List<T> {throw Exception()}
fun <T, K> g(a: Any, l: List<T>, vararg x: In<K>): Collection<T> {throw Exception()}

fun <T> test(l: List<T>) {
    f(1, <!CANNOT_INFER_PARAMETER_TYPE!>emptyList<!>())
    g(1, <!CANNOT_INFER_PARAMETER_TYPE!>emptyList<!>(), In<String>(), In<Int>())
}

fun <T> emptyList(): List<T> {throw Exception()}

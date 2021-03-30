// !WITH_NEW_INFERENCE
package f

fun <T> f(i: Int, c: Collection<T>): List<T> {throw Exception()}
fun <T> f(a: Any, l: List<T>): Collection<T> {throw Exception()}

fun <T> test(l: List<T>) {
    <!CANNOT_COMPLETE_RESOLVE{OI}, OVERLOAD_RESOLUTION_AMBIGUITY{NI}!>f<!>(1, <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>emptyList<!>())
}

fun <T> emptyList(): List<T> {throw Exception()}

//KT-743 Wrong type inference
class List<T>(val head: T, val tail: List<T>? = null)

fun <T : Any> T?.sure() : T = this!!

fun <T, Q> List<T>.map(f: (T)-> Q): List<T>? = tail.sure<List<T>>().map(f)
fun <T> foo(t : T) : T = foo(t)

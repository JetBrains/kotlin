// See KT-9529 Smart cast causes code to be incompilable

fun <T : Any> foo(o: T): Collection<T> {
    if (o is String) {
        return listOf(o)
    }
    return listOf(o)
}

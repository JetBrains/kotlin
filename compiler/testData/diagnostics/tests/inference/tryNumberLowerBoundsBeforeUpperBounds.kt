public fun <T: Any> iterate(initialValue: T, nextFunction: (T) -> T?): Iterator<T> =
        throw Exception("$initialValue $nextFunction")

fun foo() {
    iterate(3) { n -> if (n > 0) n - 1 else null }
}
// !DIAGNOSTICS: -UNUSED_PARAMETER
// KT-8132 Can't omit lambda parameter types

fun <T> test(foo: List<T>): T {
    return if (true)
        throw IllegalStateException()
    else
        foo.reduce { left, right -> left } // error: inferred type T is not subtype Nothing
}

fun <S, T: S> Iterable<T>.reduce(operation: (S, T) -> S): S = throw Exception()
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun <E> bar(x: E) {}

fun <T> foo(): T {
    val x1: T = <!INITIALIZER_TYPE_MISMATCH!>null<!>
    val x2: T? = null

    <!INAPPLICABLE_CANDIDATE!>bar<!><T>(null)
    bar<T?>(null)

    return <!RETURN_TYPE_MISMATCH!>null<!>
}

fun <T> baz(): T? = null

fun <T> foobar(): T = <!RETURN_TYPE_MISMATCH!>null<!>

class A<F> {
    fun xyz(x: F) {}

    fun foo(): F {
        val x1: F = <!INITIALIZER_TYPE_MISMATCH!>null<!>
        val x2: F? = null

        <!INAPPLICABLE_CANDIDATE!>xyz<!>(null)
        bar<F?>(null)

        return <!RETURN_TYPE_MISMATCH!>null<!>
    }

    fun baz(): F? = null

    fun foobar(): F = <!RETURN_TYPE_MISMATCH!>null<!>
}

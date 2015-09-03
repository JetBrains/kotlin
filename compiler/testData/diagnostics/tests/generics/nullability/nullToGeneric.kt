// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE,-BASE_WITH_NULLABLE_UPPER_BOUND

fun <E> bar(x: E) {}

fun <T> foo(): T {
    val x1: T = <!NULL_FOR_NONNULL_TYPE!>null<!>
    val x2: T? = null

    bar<T>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    bar<T?>(null)

    return <!NULL_FOR_NONNULL_TYPE!>null<!>
}

fun <T> baz(): T? = null

fun <T> foobar(): T = <!NULL_FOR_NONNULL_TYPE!>null<!>

class A<F> {
    fun xyz(x: F) {}

    fun foo(): F {
        val x1: F = <!NULL_FOR_NONNULL_TYPE!>null<!>
        val x2: F? = null

        xyz(<!NULL_FOR_NONNULL_TYPE!>null<!>)
        bar<F?>(null)

        return <!NULL_FOR_NONNULL_TYPE!>null<!>
    }

    fun baz(): F? = null

    fun foobar(): F = <!NULL_FOR_NONNULL_TYPE!>null<!>
}

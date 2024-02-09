class X

operator fun <T> X.iterator(): Iterable<T> = TODO()

fun test() {
    for (i in <!CANNOT_INFER_PARAMETER_TYPE, ITERATOR_MISSING!>X()<!>) {
    }
}

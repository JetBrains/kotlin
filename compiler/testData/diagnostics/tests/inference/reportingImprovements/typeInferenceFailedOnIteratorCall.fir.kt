class X

operator fun <T> X.iterator(): Iterable<T> = TODO()

fun test() {
    for (i in <!HAS_NEXT_MISSING, NEXT_MISSING!>X()<!>) {
    }
}

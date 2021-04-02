// !WITH_NEW_INFERENCE
class X

operator fun <T> X.iterator(): Iterable<T> = TODO()

fun test() {
    for (i in <!HAS_NEXT_MISSING!>X()<!>) {
    }
}

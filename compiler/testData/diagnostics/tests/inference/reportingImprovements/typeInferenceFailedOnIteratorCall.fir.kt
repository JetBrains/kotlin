class X

operator fun <T> X.iterator(): Iterable<T> = TODO()

fun test() {
    for (i in X()) {
    }
}

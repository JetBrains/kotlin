// !WITH_NEW_INFERENCE
class X

operator fun <T> X.iterator(): Iterable<T> = TODO()

fun test() {
    <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (i in X()) {
    }<!>
}
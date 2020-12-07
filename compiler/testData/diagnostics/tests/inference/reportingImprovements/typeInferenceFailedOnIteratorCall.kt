// !WITH_NEW_INFERENCE
class X

operator fun <T> X.iterator(): Iterable<T> = TODO()

fun test() {
    for (i in <!ITERATOR_MISSING{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>X()<!>) {
    }
}

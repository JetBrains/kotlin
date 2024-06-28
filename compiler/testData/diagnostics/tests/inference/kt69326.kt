// ISSUE: KT-69326

interface MyExpression<F>

fun <E> getElement(f: MyExpression<E>): E = TODO()

fun <T, S : T> MyExpression<in S>.min1(): MyExpression<T?> = TODO()
fun <T : Comparable<T>, S : T> MyExpression<in S>.min2(): MyExpression<T?> = TODO()

fun <K : Any> checkNotNull(k: K?): K = TODO()

fun foo(x: MyExpression<String>) {
    getElement(x.min1())!!.length
    checkNotNull(getElement(x.min1())).<!UNRESOLVED_REFERENCE!>length<!>

    getElement(x.min2())!!.length
    checkNotNull(<!TYPE_MISMATCH!>getElement<!>(x.<!TYPE_MISMATCH!>min2<!>())).<!UNRESOLVED_REFERENCE!>length<!>
}

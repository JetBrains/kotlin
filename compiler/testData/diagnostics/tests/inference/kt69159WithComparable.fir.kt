// ISSUE: KT-69159
interface MyExpression<F>

fun <E> getElement(f: MyExpression<E>): E = TODO()

class MyMin<T1, in S1 : T1?> : MyExpression<T1?>

fun <T2 : Comparable<T2>, S2 : T2?> MyExpression<in S2>.min(): MyMin<T2, S2> = TODO()

fun foo(x: MyExpression<String>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>getElement(x.min())!!<!>
    getElement(x.min())!!.<!UNRESOLVED_REFERENCE!>length<!>
}

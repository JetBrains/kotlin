// FIR_IDENTICAL
// ISSUE: KT-69159

interface MyExpression<F>

fun <E> getElement(f: MyExpression<E>): E = TODO()

class MyMin<T1, in S1 : T1?> : MyExpression<T1?>

fun <T2, S2 : T2?> MyExpression<in S2>.min(): MyMin<T2, S2> = TODO()

fun main(x: MyExpression<String>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>getElement(x.min())!!<!>
    getElement(x.min())!!.length
}

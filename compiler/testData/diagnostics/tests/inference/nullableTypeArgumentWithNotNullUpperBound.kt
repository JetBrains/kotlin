fun <S : Any> foo1(x: Array<out S?>, y: Array<in S?>) {
    val xo = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<S>")!>outANullable(x)<!>
    val yo = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<S>")!>inANullable(y)<!>

    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>f<!>: Array<S> = xo
    f = yo
}

fun <S : Any> foo2(x: Array<out S>, y: Array<in S>) {
    val xo = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<S>")!>outA(x)<!>
    val yo = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<S>")!>inA(y)<!>

    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>f<!>: Array<S> = xo
    f = yo
}

class A1<S : Any>(x: Array<out S?>, y: Array<in S?>) {
    val xo = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<S>")!>outANullable(x)<!>
    val yo = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<S>")!>inANullable(y)<!>
}

class A2<S : Any>(x: Array<out S>, y: Array<in S>) {
    val xo = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<S>")!>outA(x)<!>
    val yo = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<S>")!>inA(y)<!>
}

fun <X : Any> outANullable(x: Array<out X?>): Array<X> = TODO()
fun <Y : Any> inANullable(x: Array<in Y?>): Array<Y> = TODO()

fun <X : Any> outA(x: Array<out X>): Array<X> = TODO()
fun <Y : Any> inA(x: Array<in Y>): Array<Y> = TODO()

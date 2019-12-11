fun <S : Any> foo(x: Array<out S?>, y: Array<in S?>) {
    val xo = <!INAPPLICABLE_CANDIDATE!>outA<!>(x)
    val yo = inA(y)

    var f: Array<S> = xo
    f = yo
}


fun <X : Any> outA(x: Array<out X?>): Array<X> = TODO()
fun <Y : Any> inA(x: Array<in Y?>): Array<Y> = TODO()
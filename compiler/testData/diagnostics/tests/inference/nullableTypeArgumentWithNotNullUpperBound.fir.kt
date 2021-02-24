fun <S : Any> foo1(x: Array<out S?>, y: Array<in S?>) {
    val xo = outANullable(x)
    val yo = inANullable(y)

    var f: Array<S> = xo
    f = yo
}

fun <S : Any> foo2(x: Array<out S>, y: Array<in S>) {
    val xo = outA(x)
    val yo = inA(y)

    var f: Array<S> = xo
    f = yo
}

class A1<S : Any>(x: Array<out S?>, y: Array<in S?>) {
    val xo = outANullable(x)
    val yo = inANullable(y)
}

class A2<S : Any>(x: Array<out S>, y: Array<in S>) {
    val xo = outA(x)
    val yo = inA(y)
}

fun <X : Any> outANullable(x: Array<out X?>): Array<X> = TODO()
fun <Y : Any> inANullable(x: Array<in Y?>): Array<Y> = TODO()

fun <X : Any> outA(x: Array<out X>): Array<X> = TODO()
fun <Y : Any> inA(x: Array<in Y>): Array<Y> = TODO()

// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

class Outer<T> {
    inner class Inner<R> {
        fun <S> newInner(): Inner<S> = Inner()
        //type mismatch
        fun <U, S> newOuterInner(): Outer<U>.Inner<S> = Outer<U>().Inner<S>()
        //^ U here is not analyzed
        fun foo(t: T, r: R) {}
    }
}

fun test0() {
    val inner: Outer<Int>.Inner<String> = Outer<Int>().Inner<String>()
    Outer<Int>().Inner<String>().foo(1, "") // type mismatch on second argument
}


fun test1() {
    Outer<Int>().Inner<String>().newOuterInner<Double, Boolean>().foo(1.0, true) // type mismatch on 1.0
}

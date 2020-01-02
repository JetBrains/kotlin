// !DIAGNOSTICS: -UNUSED_PARAMETER

class Outer<T> {
    inner class Inner<I> {
        fun getOuter(): Outer<T> = this@Outer
        fun <R> genericFun(r: R): Outer<T> = this@Outer
    }
    fun useOuterParam(t: T) {}
}

fun test1() {
    Outer<Int>().Inner<String>().getOuter().useOuterParam(22)
    Outer<Int>().Inner<String>().getOuter().<!INAPPLICABLE_CANDIDATE!>useOuterParam<!>("")
}


class A
class B

fun test1(a: A, b: B) {
    Outer<Int>().Inner<String>().genericFun(a).Inner<Double>().genericFun(b)
}
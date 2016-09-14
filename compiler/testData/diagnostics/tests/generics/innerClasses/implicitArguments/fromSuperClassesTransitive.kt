// !CHECK_TYPE
open class Outer<X, Y> {
    inner class Inner<Z>
}

open class BaseDerived1<E, F> : Outer<F, E>()
open class BaseDerived2<X> : BaseDerived1<String, X>()

class Derived : BaseDerived2<Int>() {
    fun foo(): Inner<Char> = null!!
}

fun foo() {
    Derived().foo() checkType { _<Outer<Int, String>.Inner<Char>>() }
}

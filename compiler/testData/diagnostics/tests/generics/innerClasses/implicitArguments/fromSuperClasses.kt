// !CHECK_TYPE
open class Outer<X, Y> {
    inner class Inner<Z>
}

class Derived : Outer<String, Int>() {
    fun foo(): Inner<Char> = null!!
}


class A : Outer<Double, Short>() {
    class B : Outer<Float, Long>() {
        fun bar(): Inner<String> = null!!
    }
}

fun foo() {
    Derived().foo() checkType { _<Outer<String, Int>.Inner<Char>>() }
    A.B().bar() checkType { _<Outer<Float, Long>.Inner<String>>() }
}

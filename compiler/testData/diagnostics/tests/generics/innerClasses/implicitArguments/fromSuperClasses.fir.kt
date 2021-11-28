// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// !CHECK_TYPE
open class Outer<X, Y> {
    inner class Inner<Z>
    typealias Alias<W> = Map<W, X>
}

class Derived : Outer<String, Int>() {
    fun foo(): Inner<Char> = null!!
    fun baz(): <!UNRESOLVED_REFERENCE!>Alias<Char><!> = null!!
}


class A : Outer<Double, Short>() {
    class B : Outer<Float, Long>() {
        fun bar(): Inner<String> = null!!
        fun x(): <!UNRESOLVED_REFERENCE!>Alias<String><!> = null!!
    }
}

fun foo() {
    Derived().foo() checkType { _<Outer<String, Int>.Inner<Char>>() }
    Derived().baz() <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<Map<Char, String>>() }
    A.B().bar() checkType { _<Outer<Float, Long>.Inner<String>>() }
    A.B().x() <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<Map<String, Float>>() }
}

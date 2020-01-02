// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// !CHECK_TYPE
open class Outer<X, Y> {
    inner class Inner<Z>
    typealias Alias<W> = Map<W, X>
}

class Derived : Outer<String, Int>() {
    fun foo(): Inner<Char> = null!!
    fun baz(): Alias<Char> = null!!
}


class A : Outer<Double, Short>() {
    class B : Outer<Float, Long>() {
        fun bar(): Inner<String> = null!!
        fun x(): Alias<String> = null!!
    }
}

fun foo() {
    Derived().foo() checkType { <!UNRESOLVED_REFERENCE!>_<!><Outer<String, Int>.Inner<Char>>() }
    Derived().baz() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Map<Char, String>>() }
    A.B().bar() checkType { <!UNRESOLVED_REFERENCE!>_<!><Outer<Float, Long>.Inner<String>>() }
    A.B().x() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Map<String, Float>>() }
}

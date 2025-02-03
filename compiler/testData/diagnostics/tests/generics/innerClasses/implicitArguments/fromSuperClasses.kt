// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY -UNSUPPORTED_FEATURE
// CHECK_TYPE
open class Outer<X, Y> {
    inner class Inner<Z>
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias Alias<W> = Map<W, X>
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
    Derived().foo() checkType { _<Outer<String, Int>.Inner<Char>>() }
    Derived().baz() checkType { _<Map<Char, String>>() }
    A.B().bar() checkType { _<Outer<Float, Long>.Inner<String>>() }
    A.B().x() checkType { _<Map<String, Float>>() }
}

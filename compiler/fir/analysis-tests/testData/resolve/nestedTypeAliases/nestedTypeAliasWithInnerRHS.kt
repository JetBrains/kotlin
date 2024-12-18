// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73422

open class Outer {
    inner class Inner
    typealias NestedTA = Inner

    fun foo() {
        NestedTA()
    }
}

typealias OuterTA = Outer.NestedTA

class OuterSuccessor : Outer() {
    fun bar() {
        NestedTA()
    }
}

fun bar() {
    Outer().<!UNRESOLVED_REFERENCE!>NestedTA<!>()
    OuterSuccessor().<!UNRESOLVED_REFERENCE!>NestedTA<!>()
    Outer().OuterTA()
}

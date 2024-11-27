// RUN_PIPELINE_TILL: BACKEND
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
    Outer().NestedTA()
    OuterSuccessor().NestedTA()
    Outer().OuterTA()
}

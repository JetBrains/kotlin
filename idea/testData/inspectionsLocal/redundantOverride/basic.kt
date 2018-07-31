// PROBLEM: Redundant overriding method
// FIX: Remove redundant overriding method
open class Foo {
    open fun simple() {
    }
}

class Bar : Foo() {
    override <caret>fun simple() {
        super.simple()
    }
}

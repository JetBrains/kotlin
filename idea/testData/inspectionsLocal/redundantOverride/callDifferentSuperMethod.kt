// PROBLEM: none

open class Foo {
    open fun simple() {
    }

    open fun callDifferentSuperMethod() {
    }
}

class Bar : Foo() {
    override <caret>fun callDifferentSuperMethod() {
        super.simple()
    }
}

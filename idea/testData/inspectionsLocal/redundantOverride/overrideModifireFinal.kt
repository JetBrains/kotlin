// PROBLEM: none

open class Foo {
    open fun simple() {
    }
}

class Bar : Foo() {
    final override <caret>fun simple() {
        super.simple()
    }
}


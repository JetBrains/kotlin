// PROBLEM: none

abstract class Parent {
    protected abstract fun foo(): Nothing?
}

class Child : Parent() {
    override fun <caret>foo() = null
}
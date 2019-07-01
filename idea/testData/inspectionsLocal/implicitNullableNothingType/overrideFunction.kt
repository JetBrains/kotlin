abstract class Parent {
    protected abstract fun foo(): String?
}

class Child : Parent() {
    override fun <caret>foo() = null
}
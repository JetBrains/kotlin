open class A {
    open fun <caret>foo(n: Int): String = ""
}

class B: A() {
    override fun foo(n: Int): String = ""
}
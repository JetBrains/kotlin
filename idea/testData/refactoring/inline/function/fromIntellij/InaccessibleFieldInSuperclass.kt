open class A {
    private val i = 0

    fun <caret>foo() {
        i.toString()
    }
}

class B : A() {
    fun bar() {
        foo()
    }
}
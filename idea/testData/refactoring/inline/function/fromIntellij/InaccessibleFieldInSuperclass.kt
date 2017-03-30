class A {
    private val i = 0

    fun foo() {
        i.toString()
    }
}

class B : A() {
    fun bar() {
        <caret>foo()
    }
}
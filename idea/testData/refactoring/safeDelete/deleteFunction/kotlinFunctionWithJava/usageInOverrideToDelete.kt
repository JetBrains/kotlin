interface T {
    fun <caret>foo()
}

class A(val t: T) : T {
    override fun foo() {
        t.foo()
    }
}
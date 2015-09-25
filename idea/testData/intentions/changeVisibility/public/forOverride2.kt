interface I {
    protected fun foo()
}

abstract class C : I {
    <caret>override fun foo() {}
}
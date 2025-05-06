// FIR_IDENTICAL
interface I {
    @JsName("bar")
    fun foo()

    @JsName("foo")
    fun bar()
}

interface J {
    fun foo()

    fun bar()
}

class A : I, J {
    override fun bar() {}

    override fun foo() {}
}

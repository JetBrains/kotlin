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
    // Duplicate diagnostics are expected here, since `bar()` function gets both `foo` and `bar` names and clashes with both
    // names of `foo()` function.
    override fun bar() {}

    override fun foo() {}
}

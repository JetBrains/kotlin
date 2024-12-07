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
    // Duplicate diagnostics are expected here, since `bar()` function gets both `foo` and `bar` names and clashes with both
    // names of `foo()` function.
    <!JS_NAME_CLASH, JS_NAME_CLASH!>override fun bar()<!> {}

    <!JS_NAME_CLASH, JS_NAME_CLASH!>override fun foo()<!> {}
}

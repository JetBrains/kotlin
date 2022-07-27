// FIR_IDENTICAL
interface I {
    fun foo()
}

interface J {
    @JsName("bar")
    fun foo()
}

interface K : I, J {
    override fun foo()
}

interface L : K {
    <!JS_NAME_CLASH!>override fun foo()<!>

    <!JS_NAME_CLASH!>fun bar()<!>
}
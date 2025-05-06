// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.

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

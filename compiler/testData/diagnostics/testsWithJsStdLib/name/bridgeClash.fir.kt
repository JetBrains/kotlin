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
    override fun foo()

    fun bar()
}

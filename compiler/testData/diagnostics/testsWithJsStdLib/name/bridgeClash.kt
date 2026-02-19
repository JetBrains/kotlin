// RUN_PIPELINE_TILL: BACKEND
// In K2, the name collision detector is weakened, because the backend started to resolve such collisions.
// K1 was not changed since it's in maintenance mode.

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

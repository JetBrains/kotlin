// In K2, the name collision detector is weakened, because the backend started to resolve such collisions.
// K1 was not changed since it's in maintenance mode.

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
    <!JS_NAME_CLASH, JS_NAME_CLASH!>override fun bar()<!> {}

    <!JS_NAME_CLASH, JS_NAME_CLASH!>override fun foo()<!> {}
}

package foo

class A {
    <!JS_MODULE_PROHIBITED_ON_NON_TOPLEVEL!>@JsModule("foo")
    @native fun foo(): Int<!>
}

object B {
    <!JS_MODULE_PROHIBITED_ON_NON_TOPLEVEL!>@JsModule("foo")
    @native fun bar(): Int<!>
}

fun baz() {
    <!JS_MODULE_PROHIBITED_ON_NON_TOPLEVEL!>@JsModule("bzz")
    @native fun bzz(): Int<!>

    @JsModule("bzz")
    @native class <!JS_MODULE_PROHIBITED_ON_NON_TOPLEVEL!>C<!>
}

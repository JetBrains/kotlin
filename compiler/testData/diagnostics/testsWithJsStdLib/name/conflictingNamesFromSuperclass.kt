// FIR_IDENTICAL
interface A {
    @JsName("foo") fun f()
}

interface B {
    @JsName("foo") fun g()
}

class C : A, B {
    <!JS_NAME_CLASH!>override fun f()<!> {}

    <!JS_NAME_CLASH!>override fun g()<!> {}
}

abstract class <!JS_FAKE_NAME_CLASH!>D<!> : A, B

open class E {
    open fun f() {}

    open fun g() {}
}

class <!JS_FAKE_NAME_CLASH!>F<!> : E(), A, B

interface A {
    @JsName("f") fun foo(value: Int)
}

interface B {
    @JsName("g") fun foo(value: Int)
}

class C : A, B {
    <!JS_NAME_OVERRIDE_CLASH!>override fun foo(value: Int) { }<!>
}

@native interface NA {
    @JsName("f") fun foo(value: Int)
}

@native interface NB {
    @JsName("g") fun foo(value: Int)
}

class NC : NA, NB {
    <!JS_NAME_OVERRIDE_CLASH!>override fun foo(value: Int) { }<!>
}
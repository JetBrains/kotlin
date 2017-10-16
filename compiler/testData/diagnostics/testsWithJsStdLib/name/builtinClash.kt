class C {
    class <!JS_BUILTIN_NAME_CLASH!>prototype<!>

    class <!JS_BUILTIN_NAME_CLASH!>length<!>

    class <!JS_BUILTIN_NAME_CLASH!>`$metadata$`<!>

    <!JS_BUILTIN_NAME_CLASH!>fun constructor()<!> {}
}

class D {
    private class <!JS_BUILTIN_NAME_CLASH!>prototype<!>

    private class <!JS_BUILTIN_NAME_CLASH!>length<!>

    private class <!JS_BUILTIN_NAME_CLASH!>`$metadata$`<!>

    private fun constructor() {}
}

class E {
    @JsName("prototype")
    class <!JS_BUILTIN_NAME_CLASH!>D<!>

    <!JS_BUILTIN_NAME_CLASH!>@JsName("constructor")
    fun f()<!> {}
}

class F {
    @JsName("A")
    class prototype

    @JsName("B")
    class length

    @JsName("f")
    fun constructor() {}
}

class prototype

class length

fun constructor() {
}

fun f() {
    class prototype
    class length

    fun constructor() {}
}

external interface Object {
    val constructor: Any?
}
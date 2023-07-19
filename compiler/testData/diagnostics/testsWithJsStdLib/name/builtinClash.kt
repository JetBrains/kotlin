// FIR_IDENTICAL
// !OPT_IN: kotlin.js.ExperimentalJsExport
// FILE: f0.kt
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

class G {
    val x: String
    <!JS_BUILTIN_NAME_CLASH!>@JsName("constructor") get()<!> {
        return "1"
    }
}

class H {
    var x: String = "1"
    <!JS_BUILTIN_NAME_CLASH!>@JsName("constructor") set(v)<!> {
        field = v
    }
    @JsName("getter") get() {
        return "1"
    }
}

class I {
    <!JS_BUILTIN_NAME_CLASH!>val constructor<!> = 1
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

external interface ExternalInterface {
    fun constructor()
}

class NonExternalChild : ExternalInterface {
    <!JS_BUILTIN_NAME_CLASH!>override fun constructor()<!> {}
}

// FILE: f1.kt
package foo1

class prototype {
    companion object {
        fun test() {}
    }
}

class length {
    companion object {
        fun test() {}
    }
}

@JsExport
class C {
    class <!JS_BUILTIN_NAME_CLASH!>prototype<!>

    class <!JS_BUILTIN_NAME_CLASH!>length<!>

    class <!JS_BUILTIN_NAME_CLASH!>`$metadata$`<!>

    <!JS_BUILTIN_NAME_CLASH!>fun constructor()<!> {}
}

// FILE: f2.kt
package foo2

external class prototype {
    companion object {
        fun test()
    }
}

external class length {
    companion object {
        fun test()
    }
}

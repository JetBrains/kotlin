// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport, kotlin.js.ExperimentalJsStatic
// FILE: f0.kt
class C {
    class prototype

    class length

    class `$metadata$`

    fun constructor() {}
}

class D {
    private class prototype

    private class length

    private class `$metadata$`

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
    val constructor = 1
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

// JsStatic: previously prohibited static names as companion members
@JsExport
class ExportedStaticByJsStatic {
    companion object {
        <!JS_BUILTIN_NAME_CLASH!>@JsStatic
        fun prototype()<!> {}

        <!JS_BUILTIN_NAME_CLASH!>@JsStatic
        fun length()<!> {}

        <!JS_BUILTIN_NAME_CLASH!>@JsStatic
        fun `$metadata$`()<!> {}
    }
}

class NotExportedStaticByJsStatic {
    companion object {
        @JsStatic
        fun prototype() {}

        @JsStatic
        fun length() {}

        @JsStatic
        fun `$metadata$`() {}
    }
}

// JsStatic combined with prohibited @JsName
class StaticByJsStaticWithJsName {
    companion object {
        <!JS_BUILTIN_NAME_CLASH!>@JsStatic
        @JsName("prototype") fun f1()<!> {}

        <!JS_BUILTIN_NAME_CLASH!>@JsStatic
        @JsName("length") fun f2()<!> {}

        <!JS_BUILTIN_NAME_CLASH!>@JsStatic
        @JsName("\$metadata$") fun f3()<!> {}
    }
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

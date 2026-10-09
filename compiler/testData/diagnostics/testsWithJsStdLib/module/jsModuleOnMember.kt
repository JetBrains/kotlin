// RUN_PIPELINE_TILL: FRONTEND
package foo

// A member is referenced through its receiver, so a module annotation on it is ignored by the backend.
external class A {
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")
    fun f(): Int<!>

    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("p")
    val p: Int<!>

    <!JS_MODULE_PROHIBITED_ON_MEMBER, JS_MODULE_PROHIBITED_ON_VAR!>@JsModule("v")
    var v: Int<!>

    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsNonModule
    fun nonModule(): Int<!>

    @JsModule("B")
    class <!JS_MODULE_PROHIBITED_ON_MEMBER!>B<!>

    @JsModule("O")
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>object O<!>

    companion object {
        <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("g")
        fun g(): Int<!>
    }
}

external object Obj {
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")
    fun f(): Int<!>

    @JsModule("C")
    class <!JS_MODULE_PROHIBITED_ON_MEMBER!>C<!>
}

external interface I {
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")
    fun f(): Int<!>
}

// Deeper nesting: every level is referenced through its outer declaration.
external class Outer {
    class Middle {
        class Inner {
            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")
            fun f(): Int<!>

            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("p")
            val p: Int<!>

            @JsModule("Deepest")
            class <!JS_MODULE_PROHIBITED_ON_MEMBER!>Deepest<!>
        }

        @JsModule("Obj")
        <!JS_MODULE_PROHIBITED_ON_MEMBER!>object Obj<!> {
            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("g")
            fun g(): Int<!>
        }

        companion object {
            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("h")
            fun h(): Int<!>

            @JsModule("InCompanion")
            class <!JS_MODULE_PROHIBITED_ON_MEMBER!>InCompanion<!>
        }
    }

    interface NestedInterface {
        <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")
        fun f(): Int<!>
    }
}

// A module on the top-level declaration is fine, but not on its members at any depth.
@JsModule("TopLevel")
external class TopLevel {
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")
    fun f(): Int<!>

    @JsModule("Nested")
    class <!JS_MODULE_PROHIBITED_ON_MEMBER!>Nested<!> {
        <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("g")
        fun g(): Int<!>
    }
}

@JsModule("TopLevelObject")
external object TopLevelObject {
    object NestedObject {
        <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")
        fun f(): Int<!>
    }
}

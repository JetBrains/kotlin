// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
package foo

// A member is referenced through its receiver, so a module annotation on it is ignored by the backend.
external class A {
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")<!>
    fun f(): Int

    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("p")<!>
    val p: Int

    <!JS_MODULE_PROHIBITED_ON_VAR!><!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("v")<!>
    var v: Int<!>

    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("B")<!>
    class B

    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("O")<!>
    object O

    companion object {
        <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("g")<!>
        fun g(): Int
    }
}

external object Obj {
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")<!>
    fun f(): Int

    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("C")<!>
    class C
}

external interface I {
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")<!>
    fun f(): Int
}

// Deeper nesting: every level is referenced through its outer declaration.
external class Outer {
    class Middle {
        class Inner {
            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")<!>
            fun f(): Int

            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("p")<!>
            val p: Int

            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("Deepest")<!>
            class Deepest
        }

        <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("Obj")<!>
        object Obj {
            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("g")<!>
            fun g(): Int
        }

        companion object {
            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("h")<!>
            fun h(): Int

            <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("InCompanion")<!>
            class InCompanion
        }
    }

    interface NestedInterface {
        <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")<!>
        fun f(): Int
    }
}

// A module on the top-level declaration is fine, but not on its members at any depth.
@JsModule("TopLevel")
external class TopLevel {
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")<!>
    fun f(): Int

    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("Nested")<!>
    class Nested {
        <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("g")<!>
        fun g(): Int
    }
}

@JsModule("TopLevelObject")
external object TopLevelObject {
    object NestedObject {
        <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("f")<!>
        fun f(): Int
    }
}

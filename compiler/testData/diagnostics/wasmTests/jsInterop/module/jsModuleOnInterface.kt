// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// RENDER_DIAGNOSTICS_FULL_TEXT
package foo

// An external interface has no runtime value, so there is nothing to import from the module.
<!JS_MODULE_PROHIBITED_ON_EXTERNAL_INTERFACE!>@JsModule("I")<!>
external interface I {
    val x: Int
}

// The companion object is meant to be imported from the module, so @JsModule is not
// prohibited here.
@JsModule("WithCompanion")
external interface WithCompanion {
    companion object {
        fun ok(): String
    }
}

// A non-external interface is already covered by JS_MODULE_PROHIBITED_ON_NON_EXTERNAL.
<!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>@JsModule("NonExternal")<!>
interface NonExternal

// A nested interface is already covered by JS_MODULE_PROHIBITED_ON_MEMBER.
external class Outer {
    <!JS_MODULE_PROHIBITED_ON_MEMBER!>@JsModule("Nested")<!>
    interface Nested
}

// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// RENDER_DIAGNOSTICS_FULL_TEXT
package foo

// An external interface has no runtime value, so there is nothing to import from the module.
@JsModule("I")
external interface <!JS_MODULE_PROHIBITED_ON_EXTERNAL_INTERFACE!>I<!> {
    val x: Int
}

// The companion object is meant to be imported from the module, so @JsModule is not
// prohibited here.
// KT-87862: COMPANION_OBJECT_IN_EXTERNAL_INTERFACE is temporary (see KT-76462).
// For now the companion compiles to an empty JS object; the error goes away
// once the hack is removed.
@JsModule("WithCompanion")
external interface WithCompanion {
    companion <!COMPANION_OBJECT_IN_EXTERNAL_INTERFACE!>object<!> {
        fun ok(): String
    }
}

// A non-external interface is already covered by JS_MODULE_PROHIBITED_ON_NON_EXTERNAL.
@JsModule("NonExternal")
interface <!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>NonExternal<!>

// A nested interface is already covered by JS_MODULE_PROHIBITED_ON_MEMBER.
external class Outer {
    @JsModule("Nested")
    interface <!JS_MODULE_PROHIBITED_ON_MEMBER!>Nested<!>
}

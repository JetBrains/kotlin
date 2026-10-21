// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
package foo

// An external interface has no runtime value, so there is nothing to import from the module.
@JsModule("I")
external interface <!JS_MODULE_PROHIBITED_ON_EXTERNAL_INTERFACE!>I<!> {
    val x: Int
}

@JsNonModule
external interface <!JS_MODULE_PROHIBITED_ON_EXTERNAL_INTERFACE!>NonModule<!>

// The companion object is imported from the module.
@JsModule("WithCompanion")
external interface WithCompanion {
    companion object {
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

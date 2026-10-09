// RUN_PIPELINE_TILL: FRONTEND
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

// A non-native interface is already covered by JS_MODULE_PROHIBITED_ON_NON_NATIVE.
@JsModule("NonExternal")
interface <!JS_MODULE_PROHIBITED_ON_NON_NATIVE!>NonExternal<!>

// A nested interface is already covered by JS_MODULE_PROHIBITED_ON_MEMBER.
external class Outer {
    @JsModule("Nested")
    interface <!JS_MODULE_PROHIBITED_ON_MEMBER!>Nested<!>
}

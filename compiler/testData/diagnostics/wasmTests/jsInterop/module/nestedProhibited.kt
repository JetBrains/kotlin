// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// RENDER_DIAGNOSTICS_FULL_TEXT
@file:JsModule("foo")
package foo

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("A")<!>
external class A {
    class Nested
}

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("B")<!>
external object B

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("foo")<!>
external fun foo(): Int

<!NESTED_JS_MODULE_PROHIBITED!>@JsModule("bar")<!>
external val bar: Int

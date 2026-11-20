// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// RENDER_DIAGNOSTICS_FULL_TEXT

package foo

<!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>@JsModule("A")<!>
class A

<!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>@JsModule("B")<!>
object B

<!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>@JsModule("foo")<!>
fun foo() = 23

<!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>@JsModule("bar")<!>
val bar = 42

// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// RENDER_DIAGNOSTICS_FULL_TEXT

package foo

@JsModule("A")
class <!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>A<!>

@JsModule("B")
<!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>object B<!>

<!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>@JsModule("foo")
fun foo()<!> = 23

<!JS_MODULE_PROHIBITED_ON_NON_EXTERNAL!>@JsModule("bar")
val bar<!> = 42

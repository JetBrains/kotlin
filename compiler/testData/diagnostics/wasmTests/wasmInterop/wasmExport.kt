import kotlin.wasm.WasmExport

<!WASM_EXPORT_ON_EXTERNAL_DECLARATION!>@WasmExport("a")
external fun foo0(): Unit<!>

<!WASM_EXPORT_ON_EXTERNAL_DECLARATION!>@WasmExport("a")
fun foo1(): Int = js("42")<!>

class C() {
    <!NESTED_WASM_EXPORT!>@WasmExport("a")
    fun foo2(): Int = 42<!>
}

@WasmExport("a")
fun foo3(): Int = 42

@WasmExport()
fun foo4(): Int = 42

<!JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION!>@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport()
@WasmExport()
fun foo6(): Int = 42<!>

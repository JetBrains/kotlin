import kotlin.wasm.WasmImport

@WasmImport("a", "b")
external fun foo0(): Unit

<!WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION!>@WasmImport("a", "b")<!>
fun foo1() {
}

external class C {
    <!NESTED_WASM_IMPORT!>@WasmImport("a", "b")<!>
    fun memberFunction()
}

fun foo2() {
    <!NESTED_WASM_IMPORT, WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION!>@WasmImport("a", "b")<!>
    fun localFun() {
    }
}

val p1 = (<!NESTED_WASM_IMPORT, WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION!>@WasmImport("a", "b")<!> fun () {})

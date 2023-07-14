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

val p1 = (<!NESTED_WASM_EXPORT!>@WasmExport("a") fun () {}<!>)

@WasmExport("a")
fun foo7(
<!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p0: Unit<!>,
<!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p1: String<!>,
<!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p2: Any<!>,
<!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p3: Int?<!>,
<!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p4: Boolean?<!>
): Unit {
    p0.toString()
    p1.toString()
    p2.toString()
    p3.toString()
    p4.toString()
}

<!WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE!>@WasmExport("a")
fun returnNullableUnit(): Unit? { return null }<!>

<!WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE!>@WasmExport("a")
fun returnNullableBoolean(): Boolean? { return null }<!>

<!WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE!>@WasmExport("a")
fun returnNullableAny(): Any?  { return null }<!>

<!WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE!>@WasmExport("a")
fun <T> fooGeneric(<!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>x: T<!>): T { return x }<!>

@WasmExport("a")
fun fooDeafultAndVararg(
<!WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE!><!UNUSED_PARAMETER!>a<!>: Int = <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!><!>,
<!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE, WASM_IMPORT_EXPORT_VARARG_PARAMETER!>vararg b: Int<!>
): Unit { b.toString() }

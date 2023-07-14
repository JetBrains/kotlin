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

@WasmImport("a", "b")
external fun foo3(
    <!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p0: Unit<!>,
    <!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p1: String<!>,
    <!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p2: Any<!>,
    <!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p3: Int?<!>,
    <!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>p4: Boolean?<!>
): Unit


<!WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE!>@WasmImport("a", "b")
external fun returnNullableUnit(): Unit?<!>

<!WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE!>@WasmImport("a", "b")
external fun returnNullableBoolean(): Boolean?<!>

<!WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE!>@WasmImport("a", "b")
external fun returnNullableAny(): Any?<!>

<!WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE!>@WasmImport("a", "b")
external fun <T> fooGeneric(<!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE!>x: T<!>): T<!>

@WasmImport("a", "b")
external fun fooDeafultAndVararg(
<!WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE!>a: Int = definedExternally<!>,
<!WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE, WASM_IMPORT_EXPORT_VARARG_PARAMETER!>vararg b: Int<!>
): Unit

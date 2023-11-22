// FIR_IDENTICAL
import kotlin.wasm.WasmImport

<!WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT!>external fun foo(): Int<!>

external interface I {
    <!WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION!>fun foo(): Int<!>
}

@WasmImport("a", "b")
external fun importedFoo(): Int

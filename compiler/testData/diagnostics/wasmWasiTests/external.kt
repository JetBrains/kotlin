// FIR_IDENTICAL
import kotlin.wasm.WasmImport

<!WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT!>external fun foo(): Int<!>

<!WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION!>external interface I {
    fun foo(): Int
}<!>

<!WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION!>external class X<!>

<!WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION!>external val v: Int<!>

external <!WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION!>object AC<!>

@WasmImport("a", "b")
external fun importedFoo(): Int

import kotlin.wasm.*

// please check whether exported suspend functions are OK for WASM (Zalim told me that they should be prohibited)
@WasmExport
suspend fun myBox(): Int = 0

fun box(): String {
    return "OK"
}
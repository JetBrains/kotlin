import kotlin.wasm.*

@WasmExport
suspend fun myBox(): Int = 0

fun box(): String {
    return "OK"
}
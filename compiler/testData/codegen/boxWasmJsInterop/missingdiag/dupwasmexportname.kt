import kotlin.wasm.*

@WasmExport("foo")
fun foo1(): Int = 1

@WasmExport("foo")
fun foo2(): Int = 2

fun box(): String {
    return "OK"
}

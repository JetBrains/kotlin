import kotlin.wasm.*

@WasmExport("foo")
fun foo1(): Int = 1

// an error for a duplicate exported name could be useful
@WasmExport("foo")
fun foo2(): Int = 2

fun box(): String {
    return "OK"
}

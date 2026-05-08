// WASM_IGNORE_FOR: vm=WasmEdge
// WASM_IGNORE_FOR: vm=NodeJs
// WASM_IGNORE_FOR: vm=Wasmtime

typealias S = String

typealias SF<T> = (T) -> S

val f: SF<S> = { it }

fun box(): S = f("OK")

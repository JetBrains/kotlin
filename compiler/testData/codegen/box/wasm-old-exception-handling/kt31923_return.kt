// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_IGNORE_FOR: vm=Wasmtime
// WASM_IGNORE_FOR: vm=WasmEdge

var result = ""

fun test() {
    try {
        try {
            result += "try"
            return
        } catch (fail: Throwable) {
            result += " catch"
        }
    } finally {
        result += " finally"
        throw RuntimeException()
    }
}

fun box(): String {
    try {
        test()
        return "fail: expected exception"
    } catch (e: RuntimeException) {

    }

    return if (result == "try finally") "OK" else "fail: $result"
}

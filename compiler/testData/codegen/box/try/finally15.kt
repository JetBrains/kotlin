// WASM_IGNORE_FOR: vm=WasmEdge

fun box(): String {
    var result = "fail"

    try {
        var x: Any = 42

        try {
            try {
                throw Error()
            } finally {
                x = "OK"
            }
            x = 117
        } finally {
            result = x.toString()
        }
    } catch (_: Throwable) { }

    return result
}

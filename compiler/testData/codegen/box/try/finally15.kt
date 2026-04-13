// IGNORE_BACKEND: WASM_WASI

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

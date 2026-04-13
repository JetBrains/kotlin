// IGNORE_BACKEND: WASM_WASI

fun box(): String {
    var result = "fail"

    try {
        var x: Any = "OK"

        try {
            throw Error()
            x = 42
        } catch (e: Throwable) {
            throw Error()
            x = 43
        } finally {
            result = x.toString()
        }
    } catch (_: Throwable) { }

    return result
}

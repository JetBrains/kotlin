// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WASM_MUTE_REASON: Null leaking is not allowed by Wasm

fun <T: Any?> nullableFun(): T {
    return null as T
}

fun box(): String {
    val t = nullableFun<String>()
    return if (t?.length == null) "OK" else "Fail"
}

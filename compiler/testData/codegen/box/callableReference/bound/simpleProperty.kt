// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
fun box(): String {
    val f = "kotlin"::length
    val result = f.get()
    return if (result == 6) "OK" else "Fail: $result"
}

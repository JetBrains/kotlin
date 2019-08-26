// IGNORE_BACKEND: WASM
fun box(): String {
    val k = object {
        val ok = "OK"
    }

    return k.ok
}

// IGNORE_BACKEND: WASM
fun box(): String {
    lateinit var ok: String
    run {
        ok = "OK"
    }
    return ok
}

// IGNORE_BACKEND: WASM
fun runNoInline(f: () -> Unit) = f()

fun box(): String {
    lateinit var ok: String
    runNoInline {
        ok = "OK"
    }
    return ok
}

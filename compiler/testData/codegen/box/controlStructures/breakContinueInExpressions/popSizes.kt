// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun foo(x: Long, y: Int, z: Double, s: String) {}

fun box(): String {
    while (true) {
        try {
            foo(0, 0, 0.0, "" + continue)
        }
        finally {
            foo(0, 0, 0.0, "" + break)
        }
    }
    return "OK"
}

// IGNORE_BACKEND: WASM
fun box(): String {
    if (!true) {
        return "fail"
    } else {
        return "OK"
    }
}

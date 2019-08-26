// IGNORE_BACKEND: WASM
fun box(): String {
    var cycle = true;
    while (true) {
        if (true || throw RuntimeException()) {
            return "OK"
        }
    }
    return "fail"
}

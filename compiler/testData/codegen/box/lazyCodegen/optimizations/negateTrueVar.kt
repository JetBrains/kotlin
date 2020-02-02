// IGNORE_BACKEND: WASM
fun box(): String {
    var p = 1 < 2;
    if (!p) {
        return "fail"
    } else {
        return "OK"
    }
}
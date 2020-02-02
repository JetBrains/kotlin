// IGNORE_BACKEND: WASM
fun box(): String {
    while (true) {
        try {
            continue;
        }
        finally {
            break;
        }
    }
    return "OK"
}
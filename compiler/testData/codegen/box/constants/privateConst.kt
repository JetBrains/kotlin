// IGNORE_BACKEND: WASM
private const val z = "OK";

fun box(): String {
    return {
        z
    }()
}
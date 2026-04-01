// IGNORE_BACKEND: WASM_JS, WASM_WASI

@Suppress("UNCHECKED_CAST")
fun <T> uncheckedCastNull(): T = null as T

fun box(): String {
    val value: Int? = uncheckedCastNull<Int>()
    return if (value != null) "FAIL: $value" else "OK"
}
